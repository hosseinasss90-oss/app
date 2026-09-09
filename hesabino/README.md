# حسابینو (Hesabino)

اپ مدیریت مالی شخصی برای ایران — **Android-only**، کاملاً روی دستگاه، با ثبت دستی سریع و استخراج تراکنش از SMS بانکی + قوانین اتوماسیون.

> این محصول برای انتشار در Google Play طراحی نشده؛ اما از نظر معماری، UX، امنیت و کیفیت داده در سطح محصول تجاری است.

---

## فرض‌های قفل‌شده

| مورد | مقدار |
|---|---|
| واحد نمایش پیش‌فرض | تومان |
| ذخیره داخلی | ریال (`Long`) |
| Min SDK | ۲۶ |
| زبان | فارسی، RTL اجباری |
| تقویم نمایش | شمسی |
| SMS بدون قانون | Draft پیشنهادی |
| متن خام SMS | پیش‌فرض خاموش |

جزئیات: [`docs/00-PRODUCT-DECISIONS.md`](docs/00-PRODUCT-DECISIONS.md)

---

## پیش‌نمایش UX

فایل تعاملی (Material 3، RTL، دارک‌مد، مسیر ثبت و SMS نمونه):

[`prototype/hesabino.html`](prototype/hesabino.html)

اسناد:

- UX و وایرفریم: [`docs/01-UX-DESIGN.md`](docs/01-UX-DESIGN.md)
- معماری و اسکیما: [`docs/02-ARCHITECTURE.md`](docs/02-ARCHITECTURE.md)
- معیار پذیرش: [`docs/03-ACCEPTANCE.md`](docs/03-ACCEPTANCE.md)

---

## اجرا در Android Studio

1. Android Studio Ladybug+ با **JDK 17**
2. Open `hesabino/`
3. Sync Gradle (Wrapper با اولین Sync ساخته می‌شود اگر `gradlew` نباشد:  
   `gradle wrapper --gradle-version 8.9`)
4. Run روی دستگاه/emulator API 26+

اگر wrapper موجود نیست:

```bash
cd hesabino
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

### دسترسی SMS

از **تنظیمات → پیامک بانکی → فعال‌سازی و اسکن**. توضیح داخل اپ:

> حسابینو پیامک‌های بانکی را فقط روی همین دستگاه می‌خواند. متن به اینترنت ارسال نمی‌شود.

---

## ساختار ماژول‌ها

```
ir.hesabino.app
├── data/          Room + DataStore + Repository
├── domain/        مدل‌ها
├── engine/        Normalizer, Parser, Fingerprint, RuleEngine  (بدون Android)
├── sms/           Receiver + WorkManager import
└── ui/            Compose Material3
```

تست‌های واحد JVM روی `engine` (اعداد فارسی، مبلغ، fingerprint، matching قانون، پارسر ژنریک، تاریخ شمسی).

---

## مسیر طلایی محصول

1. **ثبت دستی ≤ ۲ تپ:** FAB → مبلغ (فوکوس) → ذخیره
2. **SMS:** Receiver → parse → dedup → قانون
3. **قانون از نمونه:** تراکنش یا Draft → Rule Builder → Dry-run
4. **اجرا:** Auto-approve یا Draft یک‌تپی
5. **عدم تکرار:** `sha256(sender|amount|last4|minuteBucket|bodyCanonical)`

مثال: SMS برداشت ۱٬۲۰۰٬۰۰۰ ریال ملت → قانون «تن ماهی / خوراک».
