# حسابینو — معماری فنی

Clean Architecture + MVVM · Kotlin · Compose · Hilt · Room · DataStore · WorkManager

---

## 1) ساختار پکیج

```
ir.hesabino.app
├── HesabinoApp.kt
├── MainActivity.kt
├── di/                      Hilt modules
├── data/
│   ├── local/
│   │   ├── db/              Database, Entity, Dao, Converters, Mappers
│   │   └── datastore/       UserPreferences
│   ├── repository/          impl
│   └── backup/              JSON/CSV
├── domain/
│   ├── model/               pure models + enums
│   ├── repository/          interfaces
│   └── usecase/
├── sms/                     Android SMS boundary
│   ├── SmsReceiver.kt
│   ├── SmsImportWorker.kt
│   └── SmsInboxReader.kt
├── engine/                  بدون وابستگی Android
│   ├── normalize/           اعداد، جداکننده، ریال/تومان
│   ├── parser/              Parser interface + بانک‌ها
│   ├── fingerprint/         Dedup
│   └── rules/               RuleEngine + DryRun
└── ui/
    ├── theme/
    ├── navigation/
    ├── components/
    ├── dashboard/
    ├── transactions/
    ├── addedit/
    ├── automations/
    ├── rules/
    ├── settings/
    ├── accounts/
    ├── categories/
    └── lock/
```

لایه `engine` تست‌پذیر است (JVM unit test) و هیچ `android.*` ندارد.

---

## 2) جریان SMS → Transaction

```
┌─────────────┐   Intent    ┌──────────────┐
│ SMS inbox / │────────────▶│ SmsReceiver  │
│ Broadcast   │             │ (main/bg)    │
└─────────────┘             └──────┬───────┘
                                   │ SmsMessage(sender, body, timestamp)
                                   ▼
                            SmsIngestor.ingest()
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
              Normalizer     ParserRegistry   Fingerprint
              fa→en digits   canParse+parse   sha256(...)
              separators     BankEventDraft
              rial/toman
                    └──────────────┬──────────────┘
                                   ▼
                          Dedup vs BankEvent
                     fingerprint UNIQUE در Room
                                   │
                    ┌──────────────┴──────────────┐
                    │ hit → STOP (idempotent)     │
                    │ miss → insert BankEvent     │
                    └──────────────┬──────────────┘
                                   ▼
                              RuleEngine
                         rules.sortedBy(priority)
                         first AND-match wins
                                   │
         ┌─────────────┬───────────┼─────────────┐
         ▼             ▼           ▼             ▼
    autoApprove    draft-mode   tie-break    no match
    → Tx SMS_RULE  → Draft      → Draft      → Draft
                                   │          (پیشنهاد ژنریک)
                                   ▼
                         UI: Automations / Snackbar
```

اسکن اولیه: `SmsImportWorker` (WorkManager, Expedited) همان `ingest()` را روی cursor `content://sms/inbox` اجرا می‌کند. Dedup جلوی تکرار را می‌گیرد.

---

## 3) DB Schema (Room v1)

همه PKها `Long` autoGenerate. زمان‌ها `epochMillis: Long`. مبلغ `amountRials: Long` (همیشه ریال، مثبت).

```
Account
  id PK
  name TEXT NOT NULL
  type TEXT  CASH|CARD|BANK|OTHER
  last4 TEXT NULL
  colorArgb INT
  sortOrder INT
  isArchived INT DEFAULT 0
  createdAt INT

Category
  id PK
  name TEXT NOT NULL
  iconKey TEXT          # grocery, transport, ...
  kind TEXT  EXPENSE|INCOME|TRANSFER|ANY
  parentId FK Category NULL
  sortOrder INT
  isArchived INT DEFAULT 0
  mergeIntoId FK Category NULL   # برای ادغام

Transaction
  id PK
  type TEXT  EXPENSE|INCOME|TRANSFER
  amountRials INT NOT NULL
  occurredAt INT NOT NULL
  accountId FK Account NOT NULL
  toAccountId FK Account NULL     # فقط TRANSFER
  categoryId FK Category NULL
  title TEXT NOT NULL
  note TEXT
  tags TEXT                       # JSON list
  createdFrom TEXT  MANUAL|SMS_RULE|SMS_DRAFT
  sourceEventId FK BankEvent NULL
  createdAt INT
  updatedAt INT
  isDeleted INT DEFAULT 0         # soft delete برای Undo

BankEvent
  id PK
  senderId TEXT NOT NULL
  bankKey TEXT NULL               # mellat, melli, generic...
  parsedType TEXT  DEBIT|CREDIT|UNKNOWN
  amountRials INT NULL
  currencyHint TEXT  RIAL|TOMAN|UNKNOWN
  cardLast4 TEXT NULL
  balanceRials INT NULL
  description TEXT NULL
  receivedAt INT NOT NULL
  fingerprint TEXT NOT NULL UNIQUE
  bodyHash TEXT NOT NULL
  rawBody TEXT NULL               # فقط اگر تنظیم روشن باشد
  parseStatus TEXT  PARSED|PARTIAL|UNPARSED
  createdAt INT

AutomationRule
  id PK
  name TEXT NOT NULL
  isEnabled INT DEFAULT 1
  autoApprove INT DEFAULT 0
  priority INT DEFAULT 100        # کمتر = زودتر
  createdAt INT
  updatedAt INT

RuleCondition
  id PK
  ruleId FK AutomationRule ON DELETE CASCADE
  field TEXT  TYPE|AMOUNT|AMOUNT_RANGE|SENDER|BANK|CARD_LAST4|BODY
  operator TEXT  EQ|NEQ|GT|LT|GTE|LTE|BETWEEN|CONTAINS|STARTS|REGEX
  value TEXT NOT NULL             # JSON-encoded (scalar یا [min,max])

RuleAction
  ruleId PK/FK AutomationRule ON DELETE CASCADE
  title TEXT NOT NULL
  categoryId FK Category NULL
  accountId FK Account NULL
  tags TEXT
  noteTemplate TEXT NULL          # {{amount}} {{date}} {{last4}}

DraftTransaction
  id PK
  bankEventId FK BankEvent NOT NULL
  ruleId FK AutomationRule NULL
  suggestedTitle TEXT
  suggestedType TEXT
  suggestedAmountRials INT
  suggestedCategoryId INT NULL
  suggestedAccountId INT NULL
  suggestedNote TEXT
  status TEXT  PENDING|APPROVED|REJECTED
  createdAt INT
  resolvedAt INT NULL
```

**ایندکس‌ها**

- `Transaction(occurredAt DESC)`, `Transaction(accountId)`, `Transaction(categoryId)`, `Transaction(sourceEventId)`
- `BankEvent(fingerprint UNIQUE)`, `BankEvent(senderId, receivedAt)`
- `DraftTransaction(status, createdAt)`, `DraftTransaction(bankEventId UNIQUE)` — یک draft باز برای هر event
- `RuleCondition(ruleId)`

**روابط**

```
Account 1──* Transaction
Category 1──* Transaction
BankEvent 1──0..1 Transaction (از طریق sourceEventId)
BankEvent 1──0..1 DraftTransaction
AutomationRule 1──* RuleCondition
AutomationRule 1──1 RuleAction
AutomationRule 1──* DraftTransaction
```

**Migration:** `fallbackToDestructiveMigration()` ممنوع در release.
v1 از روز اول با `autoMigrations` آماده است. هر تغییر schema → Migration صریح در `HesabinoDatabase`.

---

## 4) Dedup و Edge-caseها

### Fingerprint پایدار

```
fingerprint = sha256(
    senderId.normalized
  + "|" + amountRials   (یا "?" اگر parse نشد)
  + "|" + cardLast4     (یا "-")
  + "|" + timeBucket    (receivedAt / 60s)   // بانک ممکن است ۲ بار در همان دقیقه بفرستد؟ bucket دقیقه
  + "|" + bodyCanonical // متن نرمال‌شده بدون فاصلهٔ تکراری و بدون ارقام متغیر مانده؟ 
)
```

`bodyCanonical`: نرمال اعداد + collapse whitespace + حذف خط مانده اگر وجود دارد
(مانده در پیام‌های تکراری ممکن است فرق کند — پس در hash بدنهٔ canonical، خط `مانده` جدا می‌شود و در fingerprint نمی‌آید).

### سناریوها

| سناریو | رفتار |
|---|---|
| Restart اپ، SMS دوباره deliver نمی‌شود | BankEvent باقی است؛ مشکلی نیست |
| Re-import inbox | UNIQUE fingerprint → insert نادیده |
| بانک ۲ SMS مشابه در ۲ دقیقه (مانده فرق دارد) | bodyCanonical بدون مانده → یک event |
| بانک retry عین همان SMS | timeBucket یکسان → یک event |
| دو تراکنش واقعاً جدا با مبلغ یکسان | timeBucket یا body فرق دارد → دو event |
| Parse ناموفق | fingerprint از sender+bodyHash+timeBucket؛ event UNPARSED؛ Draft دستی |
| کاربر Draft را رد کرده، re-import | Draft REJECTED برای همان bankEventId؛ قانون دوباره Draft نمی‌سازد مگر `reprocessRejected` |
| کاربر Tx را دستی حذف کرده، SMS دوباره | sourceEventId هنوز به Tx soft-deleted اشاره می‌کند؛ ingest Tx جدید نمی‌سازد |

---

## 5) Rule Engine

- ارزیابی AND روی همهٔ Conditionها
- مرتب‌سازی `priority ASC, id ASC`
- اگر چند قانون با priority یکسان match شوند و تنظیم `tieBreak = DRAFT` → Draft با `ruleId = null` و note «چند قانون»
- Operatorها: EQ, NEQ, GT, LT, GTE, LTE, BETWEEN, CONTAINS (نرمال‌شده، case-insensitive برای لاتین), STARTS, REGEX
- Validation هنگام ذخیره:
  - حداقل یک Condition
  - Action.title غیرخالی
  - اگر تنها شرط `AMOUNT EQ` باشد → UI warning (ذخیره مجاز)
- Dry-run: آخرین N=20 BankEvent همان sender (یا همه اگر sender خالی) را ارزیابی و نتیجهٔ match/no را برمی‌گرداند؛ چیزی persist نمی‌شود

---

## 6) Parser Plugin

```kotlin
interface BankSmsParser {
    val key: String
    val displayName: String
    fun canParse(senderId: String, body: String): Boolean
    fun parse(senderId: String, body: String, receivedAt: Long): ParseResult
}
```

`ParserRegistry` لیست را به ترتیب اختصاصی→ژنریک می‌پیماید. اولینی که `canParse` بدهد مسئول است.

ژنریک: کلیدواژهٔ برداشت/واریز + استخراج مبلغ مقاوم + last4 با `*1234` یا `منتهی به ۱۲۳۴`.

---

## 7) امنیت On-device

- هیچ شبکهٔ پیش‌فرض. Backup فایل محلی (SAF).
- DataStore برای PIN hash (SHA-256 + salt 16B). PIN خام ذخیره نمی‌شود.
- BiometricPrompt برای باز کردن؛ PIN fallback.
- `rawBody` ستون nullable؛ اگر preference خاموش باشد همیشه null.
- SQLCipher تعمداً در v1 نیست (پیچیدگی کلید)؛ قفل UI + فایل پشتیبان اختیاری با رمز فاز ۲.

---

## 8) ترجیحات DataStore

```
displayCurrency: TOMAN|RIAL
persianDigits: Boolean = true
privacyMode: Boolean = false
storeRawSms: Boolean = false
unmatchedPolicy: DRAFT|IGNORE
tieBreak: DRAFT|FIRST
smsPermissionRequested: Boolean
appLockEnabled: Boolean
biometricEnabled: Boolean
pinHash / pinSalt
defaultAccountId
lastExpenseCategoryId / lastIncomeCategoryId
debugLogEnabled: Boolean = false
```
