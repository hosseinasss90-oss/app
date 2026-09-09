package ir.hesabino.app.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.engine.rules.RuleEngine
import ir.hesabino.app.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    onDone: () -> Unit,
    vm: RuleBuilderViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قانون خودکار") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, null) } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.name, vm::setName, modifier = Modifier.fillMaxWidth(), label = { Text("نام قانون") })
            Text("شرایط", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(state.amount, vm::setAmount, modifier = Modifier.fillMaxWidth(), label = { Text("مبلغ دقیق (واحد نمایش)") }, supportingText = { Text("خالی = هر مبلغ") })
            OutlinedTextField(state.sender, vm::setSender, modifier = Modifier.fillMaxWidth(), label = { Text("فرستنده شامل") })
            OutlinedTextField(state.bodyContains, vm::setBody, modifier = Modifier.fillMaxWidth(), label = { Text("متن شامل") })
            OutlinedTextField(state.last4, vm::setLast4, modifier = Modifier.fillMaxWidth(), label = { Text("۴ رقم آخر کارت") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.debit, onClick = { vm.setDebit(true) }, label = { Text("برداشت") })
                FilterChip(selected = !state.debit, onClick = { vm.setDebit(false) }, label = { Text("واریز") })
            }
            if (RuleEngine.isAmountOnly(state.toRule())) {
                Text("هشدار: قانون فقط روی مبلغ است و ممکن است پیام‌های نامرتبط را هم بگیرد. فرستنده یا عبارت اضافه کنید.", color = Warning, style = MaterialTheme.typography.bodyMedium)
            }
            Text("عمل", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(state.title, vm::setTitle, modifier = Modifier.fillMaxWidth(), label = { Text("عنوان تراکنش") })
            Text("دسته", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.categories.take(8).forEach { c ->
                    FilterChip(selected = state.categoryId == c.id, onClick = { vm.setCategory(c.id) }, label = { Text(c.name) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ثبت قطعی خودکار", modifier = Modifier.weight(1f))
                Switch(checked = state.autoApprove, onCheckedChange = vm::setAuto)
            }
            Text("اگر خاموش باشد، هر تطبیق به‌صورت پیش‌نویس یک‌تپی می‌آید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = vm::dryRun, modifier = Modifier.fillMaxWidth()) { Text("آزمایش روی پیام‌های اخیر") }
            state.dryRun.forEach { (label, ok) ->
                Text(if (ok) "✓ $label" else "– $label", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = vm::save, enabled = state.name.isNotBlank() && state.title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("ذخیره قانون")
            }
        }
    }
}
