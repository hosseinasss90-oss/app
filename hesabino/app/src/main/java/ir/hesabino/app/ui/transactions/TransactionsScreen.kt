package ir.hesabino.app.ui.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.ui.components.TxRow
import ir.hesabino.app.util.JalaliDate

@Composable
fun TransactionsScreen(
    onAdd: () -> Unit,
    onTx: (Long) -> Unit,
    vm: TransactionsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
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
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("جستجو در عنوان و یادداشت") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = state.typeFilter == null, onClick = { vm.setType(null) }, label = { Text("همه") })
                }
                item {
                    FilterChip(selected = state.typeFilter == TxType.EXPENSE, onClick = { vm.setType(TxType.EXPENSE) }, label = { Text("هزینه") })
                }
                item {
                    FilterChip(selected = state.typeFilter == TxType.INCOME, onClick = { vm.setType(TxType.INCOME) }, label = { Text("درآمد") })
                }
                item {
                    FilterChip(selected = state.typeFilter == TxType.TRANSFER, onClick = { vm.setType(TxType.TRANSFER) }, label = { Text("انتقال") })
                }
            }
            val grouped = state.transactions.groupBy { JalaliDate.from(it.occurredAt).formatLong(state.prefs.persianDigits) }
            LazyColumn(Modifier.fillMaxSize()) {
                grouped.forEach { (date, list) ->
                    item { Text(date, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(list, key = { it.id }) { tx ->
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
                }
                if (state.transactions.isEmpty()) {
                    item {
                        Text(
                            "تراکنشی در این بازه نیست.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
