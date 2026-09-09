package ir.hesabino.app.domain.model

enum class AccountType { CASH, CARD, BANK, OTHER }

enum class TxType { EXPENSE, INCOME, TRANSFER }

enum class CategoryKind { EXPENSE, INCOME, TRANSFER, ANY }

enum class CreatedFrom { MANUAL, SMS_RULE, SMS_DRAFT }

enum class BankEventType { DEBIT, CREDIT, UNKNOWN }

enum class ParseStatus { PARSED, PARTIAL, UNPARSED }

enum class DraftStatus { PENDING, APPROVED, REJECTED }

enum class DisplayCurrency { TOMAN, RIAL }

enum class ConditionField { TYPE, AMOUNT, AMOUNT_RANGE, SENDER, BANK, CARD_LAST4, BODY }

enum class ConditionOperator { EQ, NEQ, GT, LT, GTE, LTE, BETWEEN, CONTAINS, STARTS, REGEX }

enum class UnmatchedPolicy { DRAFT, IGNORE }

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val last4: String? = null,
    val colorArgb: Int = 0xFF0F6E56.toInt(),
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val kind: CategoryKind,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
)

data class Transaction(
    val id: Long = 0,
    val type: TxType,
    val amountRials: Long,
    val occurredAt: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val title: String,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdFrom: CreatedFrom = CreatedFrom.MANUAL,
    val sourceEventId: Long? = null,
)

data class BankEvent(
    val id: Long = 0,
    val senderId: String,
    val bankKey: String?,
    val parsedType: BankEventType,
    val amountRials: Long?,
    val currencyHint: AmountParserUnit,
    val cardLast4: String?,
    val balanceRials: Long?,
    val description: String?,
    val receivedAt: Long,
    val fingerprint: String,
    val bodyHash: String,
    val rawBody: String?,
    val parseStatus: ParseStatus,
)

enum class AmountParserUnit { RIAL, TOMAN, UNKNOWN }

data class AutomationRule(
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val autoApprove: Boolean = false,
    val priority: Int = 100,
    val conditions: List<RuleCondition> = emptyList(),
    val action: RuleAction? = null,
)

data class RuleCondition(
    val id: Long = 0,
    val field: ConditionField,
    val operator: ConditionOperator,
    val value: String,
)

data class RuleAction(
    val title: String,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val tags: List<String> = emptyList(),
    val noteTemplate: String? = null,
)

data class DraftTransaction(
    val id: Long = 0,
    val bankEventId: Long,
    val ruleId: Long?,
    val suggestedTitle: String,
    val suggestedType: TxType,
    val suggestedAmountRials: Long,
    val suggestedCategoryId: Long?,
    val suggestedAccountId: Long?,
    val suggestedNote: String?,
    val status: DraftStatus = DraftStatus.PENDING,
)

data class MonthSummary(
    val expenseRials: Long,
    val incomeRials: Long,
    val netRials: Long,
    val byCategory: List<CategorySpend>,
) {
    data class CategorySpend(
        val categoryId: Long?,
        val name: String,
        val iconKey: String,
        val amountRials: Long,
        val share: Float,
    )
}

data class UserPrefs(
    val displayCurrency: DisplayCurrency = DisplayCurrency.TOMAN,
    val persianDigits: Boolean = true,
    val privacyMode: Boolean = false,
    val storeRawSms: Boolean = false,
    val unmatchedPolicy: UnmatchedPolicy = UnmatchedPolicy.DRAFT,
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val defaultAccountId: Long? = null,
    val lastExpenseCategoryId: Long? = null,
    val lastIncomeCategoryId: Long? = null,
    val debugLogEnabled: Boolean = false,
)
