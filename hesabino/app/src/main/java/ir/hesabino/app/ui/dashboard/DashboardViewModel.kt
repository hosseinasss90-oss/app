package ir.hesabino.app.ui.dashboard

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** نوع نمودار قابل انتخاب در داشبورد. */
enum class ChartType { DONUT, DAILY, MONTHLY, TREND }

/** یک ماه شمسی همراه بازهٔ زمانی. */
data class JalaliMonthRef(
    val year: Int,
    val month: Int,
    val start: Long,
    val end: Long,
)

/** ارزش یک ماه برای نمودار ماهانه/روند. */
data class MonthPoint(
    val ref: JalaliMonthRef,
    val expenseRials: Long,
    val incomeRials: Long,
    val netRials: Long,
    val count: Int,
)

/** یک روز برای حالت روزانه. */
data class DayPoint(
    val dayOfMonth: Int,
    val count: Int,
    val expenseRials: Long,
    val incomeRials: Long,
)

/** یک دسته همراه مبلغ برای دونات ماه. */
data class SpendSlice(val name: String, val amountRials: Long, val share: Float)

data class DashboardState(
    val prefs: UserPrefs = UserPrefs(),
    val currentExpenseRials: Long = 0,
    val currentIncomeRials: Long = 0,
    val currentNetRials: Long = 0,
    val recent: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableMonths: List<JalaliMonthRef> = emptyList(),
    val selectedMonth: JalaliMonthRef? = null,
    val series: List<MonthPoint> = emptyList(),
    val daily: List<DayPoint> = emptyList(),
    val donutSlices: List<SpendSlice> = emptyList(),
    val chartType: ChartType = ChartType.DONUT,
) {
    fun categoryName(id: Long?) = categories.firstOrNull { it.id == id }?.name ?: "بدون دسته"
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val finance: FinanceRepository,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val today = JalaliDate.now()

    private val chartType = MutableStateFlow(ChartType.DONUT)
    private val selectedYM = MutableStateFlow<Pair<Int, Int>>(today.year to today.month)

    private val seed = combine(
        prefs.prefs,
        finance.observeCategories(),
        finance.observeAllActive(),
        selectedYM,
        chartType,
    ) { p, cats, all, ym, ct -> Seed(p, cats, all, ym, ct) }

    val state = seed.map { s -> build(s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private fun build(s: Seed): DashboardState {
        val cats = s.cats
        val all = s.all

        // همهٔ ماه‌های شمسی بین اولین و آخرین تراکنش کاربر (به‌همراه ماه جاری برای حالت خالی)
        val minAt = all.minOfOrNull { it.occurredAt }
        val maxAt = all.maxOfOrNull { it.occurredAt }
        val monthSet = linkedSetOf<Pair<Int, Int>>()
        if (all.isEmpty()) {
            monthSet.add(today.year to today.month)
        } else {
            val startDate = JalaliDate.from(minAt!!)
            val endDate = JalaliDate.from(maxAt!!)
            var y = startDate.year
            var m = startDate.month
            while (y < endDate.year || (y == endDate.year && m <= endDate.month)) {
                monthSet.add(y to m)
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
            }
        }

        val available = monthSet.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .map { (yy, mm) ->
                val r = JalaliDate.monthRange(yy, mm)
                JalaliMonthRef(yy, mm, r.first, r.second)
            }

        // ماه‌های دارای تراکنش (برای سری ماهانه/روند فقط همین‌ها)
        val monthsWithData = available.filter { ref ->
            all.any { it.occurredAt in ref.start..ref.end }
        }

        // ماه انتخاب‌شده: دقیق یا نزدیک‌ترین قبل
        val selected = pickClosest(available, s.ym)

        val series = monthsWithData.map { ref ->
            val inMonth = all.filter { it.occurredAt in ref.start..ref.end }
            val expense = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amountRials }
            val income = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amountRials }
            MonthPoint(ref, expense, income, income - expense, inMonth.size)
        }

        val selRef = selected ?: available.lastOrNull() ?: JalaliDate.monthRange(today.year, today.month).let {
            JalaliMonthRef(today.year, today.month, it.first, it.second)
        }
        val selTx = all.filter { it.occurredAt in selRef.start..selRef.end }

        val daily = selTx
            .groupBy { JalaliDate.from(it.occurredAt).day }
            .map { (day, list) ->
                DayPoint(
                    dayOfMonth = day,
                    count = list.size,
                    expenseRials = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amountRials },
                    incomeRials = list.filter { it.type == TxType.INCOME }.sumOf { it.amountRials },
                )
            }
            .sortedBy { it.dayOfMonth }

        val catMap = cats.associateBy { it.id }
        val expenseSel = selTx.filter { it.type == TxType.EXPENSE }
        val totalExpense = expenseSel.sumOf { it.amountRials }
        val donut = expenseSel
            .groupBy { it.categoryId }
            .map { (id, list) ->
                val sum = list.sumOf { it.amountRials }
                SpendSlice(
                    name = id?.let { catMap[it]?.name } ?: "سایر",
                    amountRials = sum,
                    share = if (totalExpense == 0L) 0f else sum.toFloat() / totalExpense.toFloat(),
                )
            }
            .sortedByDescending { it.amountRials }

        val curRange = JalaliDate.monthRange(today.year, today.month)
        val curTx = all.filter { it.occurredAt in curRange.first..curRange.second }
        val curExp = curTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountRials }
        val curInc = curTx.filter { it.type == TxType.INCOME }.sumOf { it.amountRials }

        return DashboardState(
            prefs = s.prefs,
            currentExpenseRials = curExp,
            currentIncomeRials = curInc,
            currentNetRials = curInc - curExp,
            recent = curTx.sortedByDescending { it.occurredAt }.take(6),
            categories = cats,
            availableMonths = available,
            selectedMonth = selRef,
            series = series,
            daily = daily,
            donutSlices = donut,
            chartType = s.ct,
        )
    }

    private fun pickClosest(list: List<JalaliMonthRef>, ym: Pair<Int, Int>): JalaliMonthRef? {
        if (list.isEmpty()) return null
        val exact = list.firstOrNull { it.year == ym.first && it.month == ym.second }
        if (exact != null) return exact
        val before = list.lastOrNull { it.year < ym.first || (it.year == ym.first && it.month < ym.second) }
        return before ?: list.first()
    }

    fun togglePrivacy() {
        viewModelScope.launch { prefs.setPrivacyMode(!state.value.prefs.privacyMode) }
    }

    fun setChartType(t: ChartType) { chartType.value = t }

    fun nextMonth() {
        val cur = state.value.selectedMonth ?: return
        val nxt = state.value.availableMonths.firstOrNull {
            it.year > cur.year || (it.year == cur.year && it.month > cur.month)
        }
        if (nxt != null) selectedYM.value = nxt.year to nxt.month
    }

    fun prevMonth() {
        val cur = state.value.selectedMonth ?: return
        val prev = state.value.availableMonths.lastOrNull {
            it.year < cur.year || (it.year == cur.year && it.month < cur.month)
        }
        if (prev != null) selectedYM.value = prev.year to prev.month
    }

    private data class Seed(
        val prefs: UserPrefs,
        val cats: List<Category>,
        val all: List<Transaction>,
        val ym: Pair<Int, Int>,
        val ct: ChartType,
    )
}
