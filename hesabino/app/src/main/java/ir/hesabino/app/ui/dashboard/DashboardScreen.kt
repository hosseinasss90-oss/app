package ir.hesabino.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.ui.components.ChartPalette
import ir.hesabino.app.ui.components.DonutChart
import ir.hesabino.app.ui.components.HesabinoCard
import ir.hesabino.app.ui.components.MoneyText
import ir.hesabino.app.ui.components.TxRow
import ir.hesabino.app.ui.theme.Expense
import ir.hesabino.app.ui.theme.Income
import ir.hesabino.app.util.JalaliDate
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (j.formatLong(state.prefs.persianDigits)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("خلاصه این ماه", style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(onClick = vm::togglePrivacy) {
                        Icon(
                            if (state.prefs.privacyMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "حالت خصوصی",
                        )
                    }
                }
            }
            item {
                HesabinoCard {
                    Column {
                        Text("خالص", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(
                            rials = state.summary.netRials,
                            currency = state.prefs.displayCurrency,
                            persianDigits = state.prefs.persianDigits,
                            privacy = state.prefs.privacyMode,
                            signed = true,
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("هزینه", color = Expense, style = MaterialTheme.typography.labelSmall)
                                MoneyText(state.summary.expenseRials, state.prefs.displayCurrency, state.prefs.persianDigits, state.prefs.privacyMode, type = ir.hesabino.app.domain.model.TxType.EXPENSE, style = MaterialTheme.typography.titleLarge)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("درآمد", color = Income, style = MaterialTheme.typography.labelSmall)
                                MoneyText(state.summary.incomeRials, state.prefs.displayCurrency, state.prefs.persianDigits, state.prefs.privacyMode, type = ir.hesabino.app.domain.model.TxType.INCOME, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
            if (state.summary.byCategory.isNotEmpty()) {
                item {
                    HesabinoCard {
                        Text("هزینه‌ها بر اساس دسته", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val slices = state.summary.byCategory.take(5).mapIndexed { i, c ->
                                ChartPalette[i % ChartPalette.size] to c.share
                            }
                            val topShare = ((state.summary.byCategory.firstOrNull()?.share ?: 0f) * 100).toInt()
                            DonutChart(
                                slices = slices,
                                centerLabel = if (state.prefs.persianDigits) "$topShare٪".toPersianDigits() else "$topShare%",
                            )
                            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.summary.byCategory.take(3).forEachIndexed { i, c ->
                                    val pct = (c.share * 100).toInt()
                                    Text(
                                        "${c.name}  ${if (state.prefs.persianDigits) "$pct٪".toPersianDigits() else "$pct%"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChartPalette[i % ChartPalette.size],
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("آخرین تراکنش‌ها", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onSeeAll) { Text("همه") }
                }
            }
            if (state.recent.isEmpty()) {
                item {
                    Text(
                        "هنوز تراکنشی نیست. با دکمه + اولین مورد را ثبت کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.recent, key = { it.id }) { tx ->
                TxRow(
                    title = tx.title,
                    subtitle = state.categoryName(tx.categoryId),
                    rials = tx.amountRials,
                    type = tx.type,
                    currency = state.prefs.displayCurrency,
                    persianDigits = state.prefs.persianDigits,
                    privacy = state.prefs.privacyMode,
                    onClick = { onTx(tx.id) },
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}
