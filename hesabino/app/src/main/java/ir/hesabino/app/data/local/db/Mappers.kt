package ir.hesabino.app.data.local.db

import ir.hesabino.app.data.local.db.entity.AccountEntity
import ir.hesabino.app.data.local.db.entity.AutomationRuleEntity
import ir.hesabino.app.data.local.db.entity.BankEventEntity
import ir.hesabino.app.data.local.db.entity.CategoryEntity
import ir.hesabino.app.data.local.db.entity.DraftTransactionEntity
import ir.hesabino.app.data.local.db.entity.RuleActionEntity
import ir.hesabino.app.data.local.db.entity.RuleConditionEntity
import ir.hesabino.app.data.local.db.entity.TransactionEntity
import ir.hesabino.app.domain.model.Account
import ir.hesabino.app.domain.model.AccountType
import ir.hesabino.app.domain.model.AmountParserUnit
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.BankEvent
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.CategoryKind
import ir.hesabino.app.domain.model.ConditionField
import ir.hesabino.app.domain.model.ConditionOperator
import ir.hesabino.app.domain.model.CreatedFrom
import ir.hesabino.app.domain.model.DraftStatus
import ir.hesabino.app.domain.model.DraftTransaction
import ir.hesabino.app.domain.model.ParseStatus
import ir.hesabino.app.domain.model.RuleAction
import ir.hesabino.app.domain.model.RuleCondition
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType

private fun splitTags(json: String): List<String> =
    json.split("|").map { it.trim() }.filter { it.isNotEmpty() }

private fun joinTags(tags: List<String>): String = tags.joinToString("|")

fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), last4, colorArgb, sortOrder, isArchived)
fun Account.toEntity(now: Long) = AccountEntity(id, name, type.name, last4, colorArgb, sortOrder, isArchived, now)

fun CategoryEntity.toDomain() = Category(id, name, iconKey, CategoryKind.valueOf(kind), parentId, sortOrder, isArchived)
fun Category.toEntity() = CategoryEntity(id, name, iconKey, kind.name, parentId, sortOrder, isArchived)

fun TransactionEntity.toDomain() = Transaction(
    id, TxType.valueOf(type), amountRials, occurredAt, accountId, toAccountId, categoryId,
    title, note, splitTags(tagsJson), CreatedFrom.valueOf(createdFrom), sourceEventId,
)

fun Transaction.toEntity(now: Long, isDeleted: Boolean = false) = TransactionEntity(
    id, type.name, amountRials, occurredAt, accountId, toAccountId, categoryId,
    title, note, joinTags(tags), createdFrom.name, sourceEventId, now, now, isDeleted,
)

fun BankEventEntity.toDomain() = BankEvent(
    id, senderId, bankKey, BankEventType.valueOf(parsedType), amountRials,
    AmountParserUnit.valueOf(currencyHint), cardLast4, balanceRials, description,
    receivedAt, fingerprint, bodyHash, rawBody, ParseStatus.valueOf(parseStatus),
)

fun DraftTransactionEntity.toDomain() = DraftTransaction(
    id, bankEventId, ruleId, suggestedTitle, TxType.valueOf(suggestedType),
    suggestedAmountRials, suggestedCategoryId, suggestedAccountId, suggestedNote,
    DraftStatus.valueOf(status),
)

fun AutomationRuleEntity.toDomain(
    conditions: List<RuleConditionEntity>,
    action: RuleActionEntity?,
) = AutomationRule(
    id = id,
    name = name,
    isEnabled = isEnabled,
    autoApprove = autoApprove,
    priority = priority,
    conditions = conditions.map {
        RuleCondition(it.id, ConditionField.valueOf(it.field), ConditionOperator.valueOf(it.operator), it.value)
    },
    action = action?.let {
        RuleAction(it.title, it.categoryId, it.accountId, splitTags(it.tagsJson), it.noteTemplate)
    },
)

fun RuleAction.toEntity(ruleId: Long) = RuleActionEntity(
    ruleId, title, categoryId, accountId, joinTags(tags), noteTemplate,
)

fun RuleCondition.toEntity(ruleId: Long) = RuleConditionEntity(
    id, ruleId, field.name, operator.name, value,
)
