package ir.hesabino.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.domain.model.DisplayCurrency
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
    val j = JalaliDate.now()
    val p = state.prefs

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "ثبت تراکنش")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "حسابینو · ${j.formatLong(p.persianDigits)}",
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
            }

            item { HeroBalanceCard(state, onSeeAll) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatMini(
                        label = "هزینهٔ ماه",
                        icon = Icons.Outlined.ArrowDownward,
                        color = Expense,
                        text = displayMoney(state.summary.expenseRials, p, signed = false),
                        modifier = Modifier.weight(1f),
                    )
                    StatMini(
                        label = "درآمد ماه",
                        icon = Icons.Outlined.ArrowUpward,
                        color = Income,
                        text = displayMoney(state.summary.incomeRials, p, signed = false),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                ChartCard(
                    chartType = state.chartType,
                    onChartType = vm::setChartType,
                    state = state,
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "آخرین تراکنش‌ها",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSeeAll) { Text("مشاهدهٔ همه") }
                }
            }
            if (state.recent.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
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
            }
            items(state.recent, key = { it.id }) { tx ->
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
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun HeroBalanceCard(state: DashboardState, onSeeAll: () -> Unit) {
    val p = state.prefs
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Box(
            Modifier
                .background(
                    Brush.horizontalGradient(listOf(HeroStartLight, HeroEndLight)),
                    RoundedCornerShape(26.dp),
                )
                .padding(22.dp),
        ) {
            Column {
                Text("ماندهٔ خالص این ماه", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = displayMoney(state.summary.netRials, p, signed = true),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    HeroSub("- ${displayMoney(state.summary.expenseRials, p, signed = false)}", Color(0xFFFFD9D9))
                    HeroSub("+ ${displayMoney(state.summary.incomeRials, p, signed = false)}", Color(0xFFD9FFEA))
                }
            }
        }
    }
}

@Composable
private fun HeroSub(text: String, color: Color) {
    Text(text, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun StatMini(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.padding(8.dp).height(0.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ChartCard(chartType: ChartType, onChartType: (ChartType) -> Unit, state: DashboardState) {
    val p = state.prefs
    HesabinoCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("گزارش مالی", style = MaterialTheme.typography.titleLarge)
            Text(
                "نوع نمودار را انتخاب و روند چند ماهه را دنبال کنید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = chartType == ChartType.DONUT, onClick = { onChartType(ChartType.DONUT) }, label = { Text("دسته‌ها") })
                FilterChip(selected = chartType == ChartType.BAR, onClick = { onChartType(ChartType.BAR) }, label = { Text("ماهانه") })
                FilterChip(selected = chartType == ChartType.LINE, onClick = { onChartType(ChartType.LINE) }, label = { Text("روند خالص") })
            }

            when (chartType) {
                ChartType.DONUT -> DonutContent(state)
                ChartType.BAR -> BarContent(state)
                ChartType.LINE -> LineContent(state)
            }
        }
    }
}

@Composable
private fun DonutContent(state: DashboardState) {
    if (state.summary.byCategory.isEmpty()) {
        Text("داده‌ای برای این ماه نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        return
    }
    val byCat = state.summary.byCategory
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val slices = byCat.take(6).mapIndexed { i, c -> DonutPalette[i % DonutPalette.size] to c.share }
            val topShare = ((byCat.first().share) * 100).toInt()
            DonutChart(slices = slices, centerLabel = pctLabel(topShare, state.prefs.persianDigits))
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                byCat.take(5).forEachIndexed { i, c ->
                    val pct = (c.share * 100).toInt()
                    Text(
                        "${c.name} · ${pctLabel(pct, state.prefs.persianDigits)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DonutPalette[i % DonutPalette.size],
                    )
                }
            }
        }
        Text(
            "بزرگ‌ترین هزینهٔ این ماه: «${byCat.first().name}»",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarContent(state: DashboardState) {
    if (state.series.isEmpty()) { Text("داده‌ای موجود نیست."); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val groups = state.series.map { s ->
            ChartGroup(s.label, listOf(s.expenseRials.toFloat(), s.incomeRials.toFloat()), listOf(Expense, Income))
        }
        BarChart(groups = groups)
        ChartLegend(listOf(Expense to "هزینه", Income to "درآمد"))
        MonthLabels(state.series.map { it.label })
    }
}

@Composable
private fun LineContent(state: DashboardState) {
    if (state.series.size < 2) { Text("برای رسم روند به چند ماه داده نیاز است."); return }
    val nets = state.series.map { it.netRials.toFloat() }
    val color = if (nets.last() >= 0) Income else Expense
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LineChart(points = nets, color = color)
        ChartLegend(listOf(color to "ماندهٔ خالص"))
        MonthLabels(state.series.map { it.label })
    }
}

@Composable
private fun MonthLabels(labels: List<String>) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEach { l ->
            Text(l, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun pctLabel(pct: Int, persian: Boolean) = if (persian) "$pct٪".toPersianDigits() else "$pct%"

private fun displayMoney(rials: Long, p: ir.hesabino.app.domain.model.UserPrefs, signed: Boolean): String {
    if (p.privacyMode) return "••••"
    val unit = if (p.displayCurrency == DisplayCurrency.TOMAN) " تومان" else " ریال"
    return MoneyFormatter.format(rials, p.displayCurrency, p.persianDigits, withUnit = false, signed = signed) + unit
}


