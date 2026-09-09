package ir.hesabino.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.MonthSummary
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.util.JalaliDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val prefs: UserPrefs = UserPrefs(),
    val summary: MonthSummary = MonthSummary(0, 0, 0, emptyList()),
    val recent: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    fun categoryName(id: Long?) = categories.firstOrNull { it.id == id }?.name ?: "بدون دسته"
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val finance: FinanceRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val month = JalaliDate.now().let { JalaliDate.monthRange(it.year, it.month) }

    val state = combine(
        prefs.prefs,
        finance.observeMonthSummary(month.first, month.second),
        finance.observeTransactions(month.first, month.second),
        finance.observeCategories(),
    ) { p, s, txs, cats ->
        DashboardState(p, s, txs.take(5), cats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun togglePrivacy() {
        viewModelScope.launch {
            prefs.setPrivacyMode(!state.value.prefs.privacyMode)
        }
    }
}
