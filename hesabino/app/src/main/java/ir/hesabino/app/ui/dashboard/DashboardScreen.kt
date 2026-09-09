package ir.hesabino.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.ui.components.BarChart
import ir.hesabino.app.ui.components.ChartGroup
import ir.hesabino.app.ui.components.ChartLegend
import ir.hesabino.app.ui.components.DonutChart
import ir.hesabino.app.ui.components.DonutPalette
import ir.hesabino.app.ui.components.HesabinoCard
import ir.hesabino.app.ui.components.LineChart
import ir.hesabino.app.ui.components.MoneyText
import ir.hesabino.app.ui.components.TxRow
import ir.hesabino.app.ui.theme.Expense
import ir.hesabino.app.ui.theme.HeroEndLight
import ir.hesabino.app.ui.theme.HeroStartLight
import ir.hesabino.app.ui.theme.Income
import ir.hesabino.app.util.JalaliDate
import ir.hesabino.app.util.MoneyFormatter
import ir.hesabino.app.util.toPersianDigits

@Composable
fun DashboardScreen(
    onAdd: () -> Unit,
    onSeeAll: () -> Unit,
    onTx: (Long) -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val p = state.prefs

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "ثبت تراکنش")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "حسابینو · ${JalaliDate.now().formatLong(p.persianDigits)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("خلاصهٔ مالی", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = vm::togglePrivacy) {
                    Icon(
                        if (p.privacyMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "حالت خصوصی",
                    )
                }
            }

            HeroBalanceCard(state, onSeeAll)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatMini(
                    label = "هزینهٔ ماه",
                    icon = Icons.Outlined.ArrowDownward,
                    color = Expense,
                    text = money(state.currentExpenseRials, p, signed = false),
                    modifier = Modifier.weight(1f),
                )
                StatMini(
                    label = "درآمد ماه",
                    icon = Icons.Outlined.ArrowUpward,
                    color = Income,
                    text = money(state.currentIncomeRials, p, signed = false),
                    modifier = Modifier.weight(1f),
                )
            }

            ChartCard(
                chartType = state.chartType,
                onChartType = vm::setChartType,
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth,
                state = state,
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("مشاهدهٔ همه") }
            }
            if (state.recent.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("هنوز تراکنشی ثبت نشده است", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "با دکمهٔ + پایین صفحه، اولین هزینه یا درآمد خود را ثبت کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.recent.forEach { tx ->
                TxRow(
                    title = tx.title,
                    subtitle = state.categoryName(tx.categoryId),
                    rials = tx.amountRials,
                    type = tx.type,
                    currency = p.displayCurrency,
                    persianDigits = p.persianDigits,
                    privacy = p.privacyMode,
                    onClick = { onTx(tx.id) },
                )
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun HeroBalanceCard(state: DashboardState, onSeeAll: () -> Unit) {
    val p = state.prefs
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), shadowElevation = 4.dp) {
        Box(
            Modifier
                .background(Brush.horizontalGradient(listOf(HeroStartLight, HeroEndLight)), RoundedCornerShape(26.dp))
                .padding(22.dp),
        ) {
            Column {
                Text("ماندهٔ خالص این ماه", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = money(state.currentNetRials, p, signed = true),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Text("- ${money(state.currentExpenseRials, p, signed = false)}", color = Color(0xFFFFD9D9), fontWeight = FontWeight.SemiBold)
                    Text("+ ${money(state.currentIncomeRials, p, signed = false)}", color = Color(0xFFD9FFEA), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, icon: ImageVector, color: Color, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChartCard(
    chartType: ChartType,
    onChartType: (ChartType) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    state: DashboardState,
) {
    val p = state.prefs
    HesabinoCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("گزارش مالی", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartChip(ChartType.DONUT, "دسته‌ها", chartType, onChartType)
                ChartChip(ChartType.DAILY, "روزانه", chartType, onChartType)
                ChartChip(ChartType.MONTHLY, "ماهانه", chartType, onChartType)
                ChartChip(ChartType.TREND, "روند", chartType, onChartType)
            }

            when (chartType) {
                ChartType.DONUT, ChartType.DAILY -> MonthSelector(state.selectedMonth, p, onPrev, onNext)
                ChartType.MONTHLY -> Text("محدودهٔ ماهانه بر اساس تراکنش‌های ثبت‌شده شما", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ChartType.TREND -> Text("روند ماهانه از اولین تراکنش تا امروز", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            when (chartType) {
                ChartType.DONUT -> DonutContent(state)
                ChartType.DAILY -> DailyContent(state)
                ChartType.MONTHLY -> MonthlyContent(state)
                ChartType.TREND -> TrendContent(state)
            }
        }
    }
}

@Composable
private fun ChartChip(value: ChartType, text: String, current: ChartType, onChange: (ChartType) -> Unit) {
    FilterChip(selected = current == value, onClick = { onChange(value) }, label = { Text(text) })
}

@Composable
private fun MonthSelector(ref: JalaliMonthRef?, p: UserPrefs, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = onNext, enabled = true) { Icon(Icons.Outlined.ChevronRight, "بعدی") }
        Text(
            ref?.let { "${JalaliDate.monthName(it.month)} ${it.year}".toPersianDigits() } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = onPrev, enabled = true) { Icon(Icons.Outlined.ChevronLeft, "قبلی") }
    }
}

@Composable
private fun DonutContent(state: DashboardState) {
    val p = state.prefs
    if (state.donutSlices.isEmpty()) {
        Text("در این ماه هزینه‌ای ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val slices = state.donutSlices.take(6).mapIndexed { i, s -> DonutPalette[i % DonutPalette.size] to s.share }
        val topShare = (state.donutSlices.first().share * 100).toInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(slices = slices, centerLabel = pct(topShare, p.persianDigits))
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.donutSlices.take(5).forEachIndexed { i, s ->
                    val pct = (s.share * 100).toInt()
                    Text(
                        "${s.name} · ${pct(pct, p.persianDigits)}",
                        color = DonutPalette[i % DonutPalette.size],
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Text(
            "بزرگ‌ترین هزینه: «${state.donutSlices.first().name}» — ${money(state.donutSlices.first().amountRials, p, false)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailyContent(state: DashboardState) {
    val p = state.prefs
    if (state.daily.isEmpty()) {
        Text("در این ماه تراکنشی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val totalCount = state.daily.sumOf { it.count }
    val totalExp = state.daily.sumOf { it.expenseRials }
    val totalInc = state.daily.sumOf { it.incomeRials }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تعداد تراکنش این ماه: ${pct(totalCount, p.persianDigits)}", style = MaterialTheme.typography.titleMedium)
        val groups = state.daily.map { d ->
            ChartGroup(
                label = d.dayOfMonth.toString().toPersianDigits(),
                values = listOf(d.expenseRials.toFloat(), d.incomeRials.toFloat()),
                colors = listOf(Expense, Income),
            )
        }
        BarChart(groups = groups)
        ChartLegend(listOf(Expense to "هزینه", Income to "درآمد"))
        Text(
            "جمع هزینه ${money(totalExp, p, false)} · جمع درآمد ${money(totalInc, p, false)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // روزهای فعال به همراه تعداد تراکنش هر روز
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.daily.forEach { d ->
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("روز ${d.dayOfMonth.toString().toPersianDigits()}", style = MaterialTheme.typography.labelSmall)
                        Text("${d.count.toString().toPersianDigits()} تراکنش", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyContent(state: DashboardState) {
    val p = state.prefs
    if (state.series.isEmpty()) {
        Text("داده‌ای برای نمایش نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val groups = state.series.map { s ->
            ChartGroup(
                label = monthLabel(s.ref, todayYear = JalaliDate.now().year),
                values = listOf(s.expenseRials.toFloat(), s.incomeRials.toFloat()),
                colors = listOf(Expense, Income),
            )
        }
        BarChart(groups = groups)
        ChartLegend(listOf(Expense to "هزینه", Income to "درآمد"))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.series.forEach { s ->
                Text(
                    monthLabel(s.ref, JalaliDate.now().year) + " · " + s.count.toString().toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrendContent(state: DashboardState) {
    if (state.series.size < 1) { Text("برای رسم روند به داده نیاز است."); return }
    val nets = state.series.map { it.netRials.toFloat() }
    val color = if (nets.last() >= 0) Income else Expense
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (nets.size < 2) {
            Text("برای رسم روند به حداقل دو ماه با داده نیاز است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        LineChart(points = nets, color = color)
        ChartLegend(listOf(color to "ماندهٔ خالص ماهانه"))
    }
}

private fun monthLabel(ref: JalaliMonthRef, todayYear: Int): String {
    val name = JalaliDate.monthName(ref.month)
    return if (ref.year == todayYear) name else "$name ${ref.year}".toPersianDigits()
}

private fun pct(n: Int, persian: Boolean): String = if (persian) n.toString().toPersianDigits() else n.toString()

private fun money(rials: Long, p: UserPrefs, signed: Boolean): String {
    if (p.privacyMode) return "••••"
    val unit = if (p.displayCurrency == DisplayCurrency.TOMAN) " تومان" else " ریال"
    return MoneyFormatter.format(rials, p.displayCurrency, p.persianDigits, withUnit = false, signed = signed) + unit
}
