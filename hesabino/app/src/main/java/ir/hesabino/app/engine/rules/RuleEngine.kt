package ir.hesabino.app.engine.rules

import ir.hesabino.app.domain.model.AmountParserUnit
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.BankEvent
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.ConditionField
import ir.hesabino.app.domain.model.ConditionOperator
import ir.hesabino.app.domain.model.ParseStatus
import ir.hesabino.app.domain.model.RuleCondition
import ir.hesabino.app.engine.normalize.NumberNormalizer

data class MatchResult(
    val rule: AutomationRule,
    val matched: Boolean,
)

data class EngineDecision(
    val matches: List<AutomationRule>,
    val winner: AutomationRule?,
    val tied: Boolean,
)

/**
 * تطبیق AND همه شرایط. اولویت کمتر = زودتر.
 */
object RuleEngine {

    fun decide(event: BankEvent, rules: List<AutomationRule>): EngineDecision {
        val enabled = rules.filter { it.isEnabled }
        val matched = enabled.filter { matches(event, it) }
            .sortedWith(compareBy<AutomationRule> { it.priority }.thenBy { it.id })
        if (matched.isEmpty()) return EngineDecision(emptyList(), null, tied = false)
        val topPriority = matched.first().priority
        val top = matched.filter { it.priority == topPriority }
        return if (top.size == 1) {
            EngineDecision(matched, top.first(), tied = false)
        } else {
            EngineDecision(matched, null, tied = true)
        }
    }

    fun matches(event: BankEvent, rule: AutomationRule): Boolean {
        if (rule.conditions.isEmpty()) return false
        return rule.conditions.all { matchesCondition(event, it) }
    }

    fun matchesCondition(event: BankEvent, c: RuleCondition): Boolean {
        val actual = actualValue(event, c.field) ?: return false
        return compare(actual, c.operator, c.value, c.field)
    }

    fun isAmountOnly(rule: AutomationRule): Boolean {
        val fields = rule.conditions.map { it.field }.toSet()
        return fields.isNotEmpty() && fields.all { it == ConditionField.AMOUNT || it == ConditionField.AMOUNT_RANGE }
    }

    fun dryRun(rule: AutomationRule, events: List<BankEvent>): List<Pair<BankEvent, Boolean>> =
        events.map { it to matches(it, rule) }

    private fun actualValue(event: BankEvent, field: ConditionField): String? = when (field) {
        ConditionField.TYPE -> event.parsedType.name
        ConditionField.AMOUNT, ConditionField.AMOUNT_RANGE -> event.amountRials?.toString()
        ConditionField.SENDER -> event.senderId
        ConditionField.BANK -> event.bankKey
        ConditionField.CARD_LAST4 -> event.cardLast4
        ConditionField.BODY -> event.rawBody ?: event.description
    }

    private fun compare(
        actual: String,
        op: ConditionOperator,
        expectedRaw: String,
        field: ConditionField,
    ): Boolean {
        val actualN = NumberNormalizer.normalizeText(actual)
        val expectedN = NumberNormalizer.normalizeText(expectedRaw)
        return when (op) {
            ConditionOperator.EQ -> equalsSmart(actualN, expectedN, field)
            ConditionOperator.NEQ -> !equalsSmart(actualN, expectedN, field)
            ConditionOperator.CONTAINS -> actualN.contains(expectedN, ignoreCase = true)
            ConditionOperator.STARTS -> actualN.startsWith(expectedN, ignoreCase = true)
            ConditionOperator.REGEX -> runCatching { Regex(expectedRaw, RegexOption.IGNORE_CASE).containsMatchIn(actual) }.getOrDefault(false)
            ConditionOperator.GT,
            ConditionOperator.LT,
            ConditionOperator.GTE,
            ConditionOperator.LTE,
            -> compareLong(actualN, op, expectedN)
            ConditionOperator.BETWEEN -> {
                val parts = expectedN.split("..", ",", "|").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size != 2) return false
                val v = actualN.filter { it.isDigit() }.toLongOrNull() ?: return false
                val a = parts[0].filter { it.isDigit() }.toLongOrNull() ?: return false
                val b = parts[1].filter { it.isDigit() }.toLongOrNull() ?: return false
                v in minOf(a, b)..maxOf(a, b)
            }
        }
    }

    private fun equalsSmart(actual: String, expected: String, field: ConditionField): Boolean {
        if (field == ConditionField.AMOUNT || field == ConditionField.TYPE || field == ConditionField.CARD_LAST4) {
            val a = actual.filter { it.isDigit() || it.isLetter() }.lowercase()
            val b = expected.filter { it.isDigit() || it.isLetter() }.lowercase()
            return a == b
        }
        return actual.equals(expected, ignoreCase = true)
    }

    private fun compareLong(actual: String, op: ConditionOperator, expected: String): Boolean {
        val a = actual.filter { it.isDigit() }.toLongOrNull() ?: return false
        val b = expected.filter { it.isDigit() }.toLongOrNull() ?: return false
        return when (op) {
            ConditionOperator.GT -> a > b
            ConditionOperator.LT -> a < b
            ConditionOperator.GTE -> a >= b
            ConditionOperator.LTE -> a <= b
            else -> false
        }
    }
}

/** رویداد موقتی برای dry-run / تست بدون Room. */
fun sampleBankEvent(
    senderId: String,
    type: BankEventType,
    amountRials: Long,
    body: String,
    cardLast4: String? = null,
    bankKey: String? = "generic",
    receivedAt: Long = 0L,
) = BankEvent(
    senderId = senderId,
    bankKey = bankKey,
    parsedType = type,
    amountRials = amountRials,
    currencyHint = AmountParserUnit.RIAL,
    cardLast4 = cardLast4,
    balanceRials = null,
    description = body,
    receivedAt = receivedAt,
    fingerprint = "",
    bodyHash = "",
    rawBody = body,
    parseStatus = ParseStatus.PARSED,
)
