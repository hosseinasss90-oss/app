package ir.hesabino.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.util.JalaliDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TransactionsState(
    val prefs: UserPrefs = UserPrefs(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val typeFilter: TxType? = null,
) {
    fun categoryName(id: Long?) = categories.firstOrNull { it.id == id }?.name ?: "بدون دسته"
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    finance: FinanceRepository,
    prefs: UserPreferences,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val type = MutableStateFlow<TxType?>(null)
    private val month = JalaliDate.now().let { JalaliDate.monthRange(it.year, it.month) }

    val state = combine(query, type) { q, t -> q to t }
        .flatMapLatest { (q, t) ->
            combine(
                prefs.prefs,
                finance.observeTransactions(month.first, month.second, t?.name, null, null, q),
                finance.observeCategories(),
            ) { p, txs, cats ->
                TransactionsState(p, txs, cats, q, t)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsState())

    fun setQuery(v: String) {
        query.value = v
    }

    fun setType(v: TxType?) {
        type.value = if (type.value == v) null else v
    }
}
