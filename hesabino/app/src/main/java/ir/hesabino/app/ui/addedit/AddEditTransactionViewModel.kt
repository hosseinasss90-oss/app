package ir.hesabino.app.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Account
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.CreatedFrom
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditState(
    val prefs: UserPrefs = UserPrefs(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val type: TxType = TxType.EXPENSE,
    val amountInput: String = "",
    val title: String = "",
    val note: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val editingId: Long? = null,
    val saved: Boolean = false,
    val createdFrom: CreatedFrom = CreatedFrom.MANUAL,
    val sourceEventId: Long? = null,
) {
    val canSave: Boolean
        get() = MoneyFormatter.parseUserInputToRials(amountInput, prefs.displayCurrency) != null &&
            !title.isBlank() && accountId != null
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val finance: FinanceRepository,
    private val prefsStore: UserPreferences,
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>("txId")?.takeIf { it > 0 }
    private val form = MutableStateFlow(AddEditState(editingId = editingId))

    val state = combine(
        form,
        prefsStore.prefs,
        finance.observeAccounts(),
        finance.observeCategories(),
    ) { f, p, acc, cat ->
        var next = f.copy(prefs = p, accounts = acc, categories = cat)
        if (next.accountId == null) next = next.copy(accountId = p.defaultAccountId ?: acc.firstOrNull()?.id)
        if (next.categoryId == null) {
            val last = if (next.type == TxType.INCOME) p.lastIncomeCategoryId else p.lastExpenseCategoryId
            next = next.copy(categoryId = last ?: cat.firstOrNull()?.id)
        }
        if (next.title.isBlank() && next.categoryId != null) {
            next = next.copy(title = cat.firstOrNull { it.id == next.categoryId }?.name.orEmpty())
        }
        next
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEditState())

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val tx = finance.getTransaction(editingId) ?: return@launch
                val p = prefsStore.prefs.first()
                val display = MoneyFormatter.toDisplayNumber(tx.amountRials, p.displayCurrency)
                form.update {
                    it.copy(
                        type = tx.type,
                        amountInput = display.toString(),
                        title = tx.title,
                        note = tx.note.orEmpty(),
                        categoryId = tx.categoryId,
                        accountId = tx.accountId,
                        occurredAt = tx.occurredAt,
                        createdFrom = tx.createdFrom,
                        sourceEventId = tx.sourceEventId,
                    )
                }
            }
        }
    }

    fun setType(t: TxType) = form.update { it.copy(type = t) }
    fun setAmount(v: String) = form.update { it.copy(amountInput = v.filter { ch -> ch.isDigit() || ch in "۰۱۲۳۴۵۶۷۸۹٬," }) }
    fun setTitle(v: String) = form.update { it.copy(title = v) }
    fun setNote(v: String) = form.update { it.copy(note = v) }
    fun setCategory(id: Long) {
        val name = state.value.categories.firstOrNull { it.id == id }?.name
        form.update { it.copy(categoryId = id, title = if (it.title.isBlank() || it.title == name) name.orEmpty().ifBlank { it.title } else it.title) }
    }
    fun setAccount(id: Long) = form.update { it.copy(accountId = id) }

    fun save() {
        viewModelScope.launch {
            val s = state.value
            val rials = MoneyFormatter.parseUserInputToRials(s.amountInput, s.prefs.displayCurrency) ?: return@launch
            val accountId = s.accountId ?: return@launch
            val title = s.title.ifBlank { s.categories.firstOrNull { it.id == s.categoryId }?.name ?: "تراکنش" }
            finance.upsertTransaction(
                Transaction(
                    id = s.editingId ?: 0,
                    type = s.type,
                    amountRials = rials,
                    occurredAt = s.occurredAt,
                    accountId = accountId,
                    categoryId = s.categoryId,
                    title = title,
                    note = s.note.ifBlank { null },
                    createdFrom = s.createdFrom,
                    sourceEventId = s.sourceEventId,
                ),
            )
            s.categoryId?.let { prefsStore.setLastCategory(s.type != TxType.INCOME, it) }
            prefsStore.setDefaultAccount(accountId)
            form.update { it.copy(saved = true) }
        }
    }
}
