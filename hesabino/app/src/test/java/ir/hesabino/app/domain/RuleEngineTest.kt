package ir.hesabino.app.domain

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.ConditionField
import ir.hesabino.app.domain.model.ConditionOperator
import ir.hesabino.app.domain.model.RuleAction
import ir.hesabino.app.domain.model.RuleCondition
import ir.hesabino.app.engine.rules.RuleEngine
import ir.hesabino.app.engine.rules.sampleBankEvent
import org.junit.Test

class RuleEngineTest {

    private val tuna = AutomationRule(
        id = 1,
        name = "تن ماهی",
        autoApprove = false,
        priority = 10,
        conditions = listOf(
            RuleCondition(field = ConditionField.TYPE, operator = ConditionOperator.EQ, value = "DEBIT"),
            RuleCondition(field = ConditionField.AMOUNT, operator = ConditionOperator.EQ, value = "1200000"),
            RuleCondition(field = ConditionField.SENDER, operator = ConditionOperator.CONTAINS, value = "BMJI"),
        ),
        action = RuleAction(title = "تن ماهی", categoryId = 1),
    )

    private val event = sampleBankEvent(
        senderId = "BMJI1700",
        type = BankEventType.DEBIT,
        amountRials = 1_200_000,
        body = "برداشت از کارت *1234 مبلغ 1200000 ریال",
        cardLast4 = "1234",
        bankKey = "mellat",
    )

    @Test
    fun matchesTunaRule() {
        assertThat(RuleEngine.matches(event, tuna)).isTrue()
        val d = RuleEngine.decide(event, listOf(tuna))
        assertThat(d.winner?.name).isEqualTo("تن ماهی")
        assertThat(d.tied).isFalse()
    }

    @Test
    fun differentAmountDoesNotMatch() {
        val other = event.copy(amountRials = 850_000)
        assertThat(RuleEngine.matches(other, tuna)).isFalse()
    }

    @Test
    fun containsBody() {
        val rule = tuna.copy(
            conditions = tuna.conditions + RuleCondition(
                field = ConditionField.BODY,
                operator = ConditionOperator.CONTAINS,
                value = "1234",
            ),
        )
        assertThat(RuleEngine.matches(event, rule)).isTrue()
    }

    @Test
    fun amountRange() {
        val rule = AutomationRule(
            id = 2,
            name = "بازه",
            conditions = listOf(
                RuleCondition(field = ConditionField.AMOUNT_RANGE, operator = ConditionOperator.BETWEEN, value = "1000000..1500000"),
            ),
            action = RuleAction("x"),
        )
        assertThat(RuleEngine.matches(event, rule)).isTrue()
        assertThat(RuleEngine.matches(event.copy(amountRials = 99_000), rule)).isFalse()
    }

    @Test
    fun priorityWins() {
        val low = tuna.copy(id = 2, name = "کم", priority = 50)
        val high = tuna.copy(id = 3, name = "بالا", priority = 1)
        val d = RuleEngine.decide(event, listOf(low, high))
        assertThat(d.winner?.name).isEqualTo("بالا")
    }

    @Test
    fun samePriorityIsTie() {
        val a = tuna.copy(id = 2, name = "الف", priority = 5)
        val b = tuna.copy(id = 3, name = "ب", priority = 5)
        val d = RuleEngine.decide(event, listOf(a, b))
        assertThat(d.tied).isTrue()
        assertThat(d.winner).isNull()
    }

    @Test
    fun disabledIgnored() {
        val d = RuleEngine.decide(event, listOf(tuna.copy(isEnabled = false)))
        assertThat(d.winner).isNull()
    }

    @Test
    fun amountOnlyWarning() {
        val rule = AutomationRule(
            name = "فقط مبلغ",
            conditions = listOf(
                RuleCondition(field = ConditionField.AMOUNT, operator = ConditionOperator.EQ, value = "1200000"),
            ),
        )
        assertThat(RuleEngine.isAmountOnly(rule)).isTrue()
        assertThat(RuleEngine.isAmountOnly(tuna)).isFalse()
    }

    @Test
    fun dryRun() {
        val events = listOf(event, event.copy(amountRials = 10_000, receivedAt = 2))
        val result = RuleEngine.dryRun(tuna, events)
        assertThat(result[0].second).isTrue()
        assertThat(result[1].second).isFalse()
    }
}
