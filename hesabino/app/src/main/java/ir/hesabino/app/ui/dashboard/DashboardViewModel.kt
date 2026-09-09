package ir.hesabino.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.MonthSummary
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.util.JalaliDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** نوع نمودار قابل انتخاب در داشبورد. */
enum class ChartType { DONUT, BAR, LINE }

data class JalaliMonth(val label: String, val start: Long, val end: Long)

data class MonthPoint(
    val label: String,
    val expenseRials: Long,
    val incomeRials: Long,
    val netRials: Long,
)

data class DashboardState(
    val prefs: UserPrefs = UserPrefs(),
    val summary: MonthSummary = MonthSummary(0, 0, 0, emptyList()),
    val recent: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val series: List<MonthPoint> = emptyList(),
    val chartType: ChartType = ChartType.DONUT,
) {
    fun categoryName(id: Long?) = categories.firstOrNull { it.id == id }?.name ?: "بدون دسته"
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val finance: FinanceRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val now = JalaliDate.now()
    private val currentRange = JalaliDate.monthRange(now.year, now.month)
    private val months: List<JalaliMonth> = recentMonths(6)
    private val windowStart = months.first().start
    private val windowEnd = months.last().end

    private val chartType = MutableStateFlow(ChartType.DONUT)

    private data class Base(
        val prefs: UserPrefs,
        val summary: MonthSummary,
        val recent: List<Transaction>,
        val allTx: List<Transaction>,
        val cats: List<Category>,
    )

    private val base = combine(
        prefs.prefs,
        finance.observeMonthSummary(currentRange.first, currentRange.second),
        finance.observeTransactions(currentRange.first, currentRange.second),
        finance.observeRange(windowStart, windowEnd),
        finance.observeCategories(),
    ) { p, s, txs, allTx, cats -> Base(p, s, txs, allTx, cats) }

    val state = combine(base, chartType) { b, ct ->
        val series = months.map { m ->
            val inMonth = b.allTx.filter { it.occurredAt in m.start..m.end }
            val expense = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amountRials }
            val income = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amountRials }
            MonthPoint(m.label, expense, income, income - expense)
        }
        DashboardState(b.prefs, b.summary, b.recent.take(6), b.cats, series, ct)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun togglePrivacy() {
        viewModelScope.launch { prefs.setPrivacyMode(!state.value.prefs.privacyMode) }
    }

    fun setChartType(t: ChartType) {
        chartType.value = t
    }

    /** آخرین «count» ماه شمسی تا امروز (این ماه آخرین است). */
    private fun recentMonths(count: Int): List<JalaliMonth> {
        val list = mutableListOf<JalaliMonth>()
        var y = now.year
        var m = now.month
        repeat(count) {
            val range = JalaliDate.monthRange(y, m)
            list.add(JalaliMonth(JalaliDate.monthName(m), range.first, range.second))
            m--
            if (m == 0) {
                m = 12
                y--
            }
        }
        return list.asReversed()
    }
}
