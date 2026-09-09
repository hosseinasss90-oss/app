package ir.hesabino.app.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                .imePadding()
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
            val rel = state.relevantCategories()
            if (rel.isEmpty()) {
                Text(
                    "دسته‌ای برای این نوع پیدا نشد؛ از بخش دسته‌بندی‌ها بسازید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rel.forEach { c ->
                        FilterChip(
                            selected = state.categoryId == c.id,
                            onClick = { vm.setCategory(c.id) },
                            label = { Text(c.name) },
                        )
                    }
                }
            }
            Text("حساب (اختیاری)", style = MaterialTheme.typography.labelLarge)
            if (state.accounts.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    state.accounts.forEach { a ->
                        FilterChip(
                            selected = state.accountId == a.id,
                            onClick = { vm.setAccount(a.id) },
                            label = { Text(a.name) },
                        )
                    }
                }
            } else {
                Text(
                    "حسابی ثبت نشده؛ تراکنش به حساب پیش‌فرض می‌رود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ثبت قطعی خودکار", modifier = Modifier.weight(1f))
                Switch(checked = state.autoApprove, onCheckedChange = vm::setAuto)
            }
            Text("اگر خاموش باشد، هر تطبیق به‌صورت پیش‌نویس یک‌تپی می‌آید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = vm::dryRun, modifier = Modifier.fillMaxWidth()) { Text("آزمایش قانون") }
            state.dryRun?.let { r ->
                if (r.sampleTotal == 0) {
                    Text("پیامک بانکی یا تراکنشی برای آزمایش موجود نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "نتیجه: ${r.matchedTotal.toString()} از ${r.sampleTotal.toString()} تطبیق یافت.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("پیامک‌های منطبق (${r.eventMatched.size}/${r.eventTotal})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.eventMatched.isEmpty()) {
                        Text("هیچ پیامک بانکی مطابقت ندارد. شرط فرستنده/متن فقط روی پیامک بررسی می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        r.eventMatched.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("تراکنش‌های ثبت‌شدهٔ منطبق (${r.txMatched.size}/${r.txTotal})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (r.txMatched.isEmpty()) {
                        Text("هیچ‌کدام از تراکنش‌های شما با مبلغ/نوع این قانون هم‌خوان نیست.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        r.txMatched.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (r.eventTotal == 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "توجه: برای فعال‌شدن خودکار پیامک‌ها باید از بخش تنظیمات، «فعال‌سازی و اسکن پیامک‌های قبلی» را اجرا کنید.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Button(onClick = vm::save, enabled = state.name.isNotBlank() && state.title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("ذخیره قانون")
            }
        }
    }
}
