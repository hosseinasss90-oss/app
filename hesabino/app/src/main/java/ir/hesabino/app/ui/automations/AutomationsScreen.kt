package ir.hesabino.app.ui.automations

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.ui.components.HesabinoCard
import ir.hesabino.app.ui.components.MoneyText

@Composable
fun AutomationsScreen(
    onNewRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onEditDraft: (Long) -> Unit,
    vm: AutomationsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRule, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.Add, contentDescription = "قانون جدید")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("پیش‌نویس‌ها (${state.drafts.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("قوانین (${state.rules.size})") })
            }
            if (tab == 0) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.drafts.isEmpty()) {
                        item { Text("پیش‌نویسی نیست. پیامک‌های بانکی اینجا ظاهر می‌شوند.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(state.drafts, key = { it.id }) { d ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(d.suggestedTitle, style = MaterialTheme.typography.titleLarge)
                                MoneyText(
                                    d.suggestedAmountRials,
                                    state.prefs.displayCurrency,
                                    state.prefs.persianDigits,
                                    state.prefs.privacyMode,
                                    type = d.suggestedType,
                                )
                                if (!d.suggestedNote.isNullOrBlank()) {
                                    Text(d.suggestedNote, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { vm.reject(d.id) }) { Text("رد") }
                                    OutlinedButton(onClick = { onEditDraft(d.id) }) { Text("ویرایش") }
                                    Button(onClick = { vm.approve(d.id) }) { Text("تأیید") }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.rules.isEmpty()) {
                        item { Text("هنوز قانونی ندارید. از یک پیش‌نویس یا تراکنش، قانون بسازید.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(state.rules, key = { it.id }) { r ->
                        HesabinoCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(r.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        buildString {
                                            append(if (r.autoApprove) "ثبت خودکار" else "پیش‌نویس")
                                            append(" · ")
                                            append("${r.conditions.size} شرط")
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AssistChip(onClick = { onEditRule(r.id) }, label = { Text("ویرایش") })
                                }
                                Switch(checked = r.isEnabled, onCheckedChange = { vm.toggle(r.id, it) })
                            }
                        }
                    }
                }
            }
        }
    }
}
