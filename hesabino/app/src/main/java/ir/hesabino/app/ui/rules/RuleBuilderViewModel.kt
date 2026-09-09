package ir.hesabino.app.ui.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.AutomationRepository
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.ConditionField
import ir.hesabino.app.domain.model.ConditionOperator
import ir.hesabino.app.domain.model.RuleAction
import ir.hesabino.app.domain.model.RuleCondition
import ir.hesabino.app.engine.rules.RuleEngine
import ir.hesabino.app.util.JalaliDate
import ir.hesabino.app.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleBuilderState(
    val ruleId: Long = 0,
    val name: String = "",
    val title: String = "",
    val amount: String = "",
    val sender: String = "",
    val bodyContains: String = "",
    val last4: String = "",
    val debit: Boolean = true,
    val autoApprove: Boolean = false,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val categories: List<Category> = emptyList(),
    val dryRun: List<Pair<String, Boolean>> = emptyList(),
    val saved: Boolean = false,
    val displayCurrency: ir.hesabino.app.domain.model.DisplayCurrency =
        ir.hesabino.app.domain.model.DisplayCurrency.TOMAN,
) {
    fun toRule(): AutomationRule {
        val conds = buildList {
            add(
                RuleCondition(
                    field = ConditionField.TYPE,
                    operator = ConditionOperator.EQ,
                    value = if (debit) BankEventType.DEBIT.name else BankEventType.CREDIT.name,
                ),
            )
            if (amount.isNotBlank()) {
                val rials = MoneyFormatter.parseUserInputToRials(amount, displayCurrency)
                if (rials != null) {
                    add(RuleCondition(field = ConditionField.AMOUNT, operator = ConditionOperator.EQ, value = rials.toString()))
                }
            }
            if (sender.isNotBlank()) add(RuleCondition(field = ConditionField.SENDER, operator = ConditionOperator.CONTAINS, value = sender))
            if (bodyContains.isNotBlank()) add(RuleCondition(field = ConditionField.BODY, operator = ConditionOperator.CONTAINS, value = bodyContains))
            if (last4.isNotBlank()) add(RuleCondition(field = ConditionField.CARD_LAST4, operator = ConditionOperator.EQ, value = last4))
        }
        return AutomationRule(
            id = ruleId,
            name = name,
            autoApprove = autoApprove,
            conditions = conds,
            action = RuleAction(title = title, categoryId = categoryId, accountId = accountId),
        )
    }
}

@HiltViewModel
class RuleBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val automation: AutomationRepository,
    finance: FinanceRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val form = MutableStateFlow(RuleBuilderState())

    val state = combine(form, finance.observeCategories(), prefs.prefs) { f, cats, p ->
        f.copy(categories = cats, displayCurrency = p.displayCurrency, categoryId = f.categoryId ?: cats.firstOrNull()?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RuleBuilderState())

    init {
        val ruleId = savedStateHandle.get<Long>("ruleId") ?: 0L
        val sampleTx = savedStateHandle.get<Long>("sampleTxId") ?: 0L
        val sampleDraft = savedStateHandle.get<Long>("sampleDraftId") ?: 0L
        viewModelScope.launch {
            when {
                ruleId > 0 -> {
                    val r = automation.loadRules().firstOrNull { it.id == ruleId } ?: return@launch
                    form.update {
                        it.copy(
                            ruleId = r.id,
                            name = r.name,
                            title = r.action?.title.orEmpty(),
                            autoApprove = r.autoApprove,
                            categoryId = r.action?.categoryId,
                            amount = r.conditions.firstOrNull { c -> c.field == ConditionField.AMOUNT }?.value
                                ?.toLongOrNull()
                                ?.let { rials ->
                                    MoneyFormatter.toDisplayNumber(
                                        rials,
                                        ir.hesabino.app.domain.model.DisplayCurrency.TOMAN,
                                    ).toString()
                                }
                                .orEmpty(),
                            sender = r.conditions.firstOrNull { c -> c.field == ConditionField.SENDER }?.value.orEmpty(),
                            bodyContains = r.conditions.firstOrNull { c -> c.field == ConditionField.BODY }?.value.orEmpty(),
                            last4 = r.conditions.firstOrNull { c -> c.field == ConditionField.CARD_LAST4 }?.value.orEmpty(),
                            debit = r.conditions.firstOrNull { c -> c.field == ConditionField.TYPE }?.value != BankEventType.CREDIT.name,
                        )
                    }
                }
                sampleTx > 0 -> {
                    val tx = finance.getTransaction(sampleTx) ?: return@launch
                    val p = prefs.prefs.first()
                    form.update {
                        it.copy(
                            name = "قانون ${tx.title}",
                            title = tx.title,
                            amount = MoneyFormatter.toDisplayNumber(tx.amountRials, p.displayCurrency).toString(),
                            categoryId = tx.categoryId,
                            accountId = tx.accountId,
                            debit = tx.type.name != "INCOME",
                        )
                    }
                }
                sampleDraft > 0 -> {
                    val d = automation.getDraft(sampleDraft) ?: return@launch
                    val ev = automation.eventById(d.bankEventId)
                    val p = prefs.prefs.first()
                    form.update {
                        it.copy(
                            name = "قانون ${d.suggestedTitle}",
                            title = d.suggestedTitle,
                            amount = MoneyFormatter.toDisplayNumber(d.suggestedAmountRials, p.displayCurrency).toString(),
                            sender = ev?.senderId.orEmpty(),
                            last4 = ev?.cardLast4.orEmpty(),
                            categoryId = d.suggestedCategoryId,
                            accountId = d.suggestedAccountId,
                            debit = d.suggestedType.name != "INCOME",
                            bodyContains = "",
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) = form.update { it.copy(name = v) }
    fun setTitle(v: String) = form.update { it.copy(title = v) }
    fun setAmount(v: String) = form.update { it.copy(amount = v) }
    fun setSender(v: String) = form.update { it.copy(sender = v) }
    fun setBody(v: String) = form.update { it.copy(bodyContains = v) }
    fun setLast4(v: String) = form.update { it.copy(last4 = v.filter { ch -> ch.isDigit() }.take(4)) }
    fun setDebit(v: Boolean) = form.update { it.copy(debit = v) }
    fun setAuto(v: Boolean) = form.update { it.copy(autoApprove = v) }
    fun setCategory(id: Long) = form.update { it.copy(categoryId = id) }

    fun dryRun() {
        viewModelScope.launch {
            val events = automation.recentEvents(12)
            val rule = state.value.toRule()
            val rows = RuleEngine.dryRun(rule, events).map { (e, ok) ->
                val date = JalaliDate.from(e.receivedAt).format(false)
                "$date · ${e.senderId} · ${e.amountRials ?: "-"}" to ok
            }
            form.update { it.copy(dryRun = rows) }
        }
    }

    fun save() {
        viewModelScope.launch {
            automation.saveRule(state.value.toRule())
            form.update { it.copy(saved = true) }
        }
    }
}
