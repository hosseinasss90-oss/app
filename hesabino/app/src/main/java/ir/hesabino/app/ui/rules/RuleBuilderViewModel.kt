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
    val accounts: List<ir.hesabino.app.domain.model.Account> = emptyList(),
    val dryRun: DryRunResult? = null,
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

    /** دسته‌هایی که با جهت این قانون (برداشت/واریز) سازگارند. */
    fun relevantCategories(): List<Category> = categories.filter { c ->
        if (debit) c.kind.name in setOf("EXPENSE", "ANY") else c.kind.name in setOf("INCOME", "ANY")
    }
}

/** نتیجهٔ آزمایش (dry-run): چند پیامک/تراکنش با این قانون منطبق می‌شوند. */
data class DryRunResult(
    val eventMatched: List<String>,
    val eventTotal: Int,
    val txMatched: List<String>,
    val txTotal: Int,
) {
    val matchedTotal: Int get() = eventMatched.size + txMatched.size
    val sampleTotal: Int get() = eventTotal + txTotal
}

@HiltViewModel
class RuleBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val automation: AutomationRepository,
    finance: FinanceRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val form = MutableStateFlow(RuleBuilderState())

    val state = combine(form, finance.observeCategories(), finance.observeAccounts(), prefs.prefs) { f, cats, acc, p ->
        val relevant = cats.filter { c ->
            if (f.debit) c.kind.name in setOf("EXPENSE", "ANY") else c.kind.name in setOf("INCOME", "ANY")
        }
        val keepCat = f.categoryId?.takeIf { id -> relevant.any { it.id == id } }
            ?: f.categoryId?.takeIf { id -> cats.any { it.id == id } } // گرچه نامرتبط، اما اگر از پیش تعیین شده نگه بداریم
        f.copy(
            categories = cats,
            accounts = acc,
            displayCurrency = p.displayCurrency,
            accountId = f.accountId ?: p.defaultAccountId ?: acc.firstOrNull()?.id,
            categoryId = keepCat ?: relevant.firstOrNull()?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RuleBuilderState())

    init {
        val ruleId = savedStateHandle.get<Long>("ruleId") ?: 0L
        val sampleTx = savedStateHandle.get<Long>("sampleTxId") ?: 0L
        val sampleDraft = savedStateHandle.get<Long>("sampleDraftId") ?: 0L
        viewModelScope.launch {
            when {
                ruleId > 0 -> {
                    val r = automation.loadRules().firstOrNull { it.id == ruleId } ?: return@launch
                    val cur = prefs.prefs.first().displayCurrency
                    form.update {
                        it.copy(
                            ruleId = r.id,
                            name = r.name,
                            title = r.action?.title.orEmpty(),
                            autoApprove = r.autoApprove,
                            categoryId = r.action?.categoryId,
                            accountId = r.action?.accountId,
                            amount = r.conditions.firstOrNull { c -> c.field == ConditionField.AMOUNT }?.value
                                ?.toLongOrNull()
                                ?.let { rials ->
                                    MoneyFormatter.toDisplayNumber(rials, cur).toString()
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
    fun setDebit(v: Boolean) {
        form.update { it.copy(debit = v, categoryId = null) }
    }

    fun setAuto(v: Boolean) = form.update { it.copy(autoApprove = v) }
    fun setCategory(id: Long) = form.update { it.copy(categoryId = id) }
    fun setAccount(id: Long) = form.update { it.copy(accountId = id) }

    fun dryRun() {
        viewModelScope.launch {
            val f = state.value
            val rule = f.toRule()
            val cur = f.displayCurrency
            val fmt = { rials: Long? ->
                MoneyFormatter.format(rials ?: 0L, cur, persianDigits = false, withUnit = true)
            }

            // ۱) پیامک‌های بانکی واقعی (فقط همین‌ها برای شرط فرستنده/متن/کارت قابل‌ارزیابی‌اند)
            val events = automation.recentEvents(25)
            val matchedE = events.filter { RuleEngine.matches(it, rule) }.map { e ->
                "${JalaliDate.from(e.receivedAt).format(false)} · ${e.senderId} · ${fmt(e.amountRials)}"
            }

            // ۲) تراکنش‌های ثبت‌شدهٔ کاربر (برای اعتبارسنجی شرط مبلغ/نوع روی دادهٔ خودِ کاربر)
            val tx = finance.allTransactions().take(40)
            val matchedT = tx.filter { tx ->
                val event = probeFromTx(tx)
                RuleEngine.matches(event, rule)
            }.map { tx ->
                "${JalaliDate.from(tx.occurredAt).format(false)} · ${tx.title} · ${fmt(tx.amountRials)}"
            }

            form.update {
                it.copy(
                    dryRun = DryRunResult(
                        eventMatched = matchedE,
                        eventTotal = events.size,
                        txMatched = matchedT,
                        txTotal = tx.size,
                    ),
                )
            }
        }
    }

    private fun probeFromTx(tx: ir.hesabino.app.domain.model.Transaction) =
        ir.hesabino.app.engine.rules.sampleBankEvent(
            senderId = "",
            type = when (tx.type) {
                ir.hesabino.app.domain.model.TxType.EXPENSE -> BankEventType.DEBIT
                ir.hesabino.app.domain.model.TxType.INCOME -> BankEventType.CREDIT
                else -> BankEventType.DEBIT
            },
            amountRials = tx.amountRials,
            body = tx.title,
            receivedAt = tx.occurredAt,
        )

    fun save() {
        viewModelScope.launch {
            automation.saveRule(state.value.toRule())
            form.update { it.copy(saved = true) }
        }
    }
}
