package ir.hesabino.app.ui.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.util.JalaliDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    onCreateRule: (Long) -> Unit,
    vm: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = FocusRequester()
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId == null) "ثبت تراکنش" else "ویرایش") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Outlined.Close, "بستن") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val types = listOf(TxType.EXPENSE to "هزینه", TxType.INCOME to "درآمد", TxType.TRANSFER to "انتقال")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, (t, label) ->
                    SegmentedButton(
                        selected = state.type == t,
                        onClick = { vm.setType(t) },
                        shape = SegmentedButtonDefaults.itemShape(i, types.size),
                    ) { Text(label) }
                }
            }
            OutlinedTextField(
                value = state.amountInput,
                onValueChange = vm::setAmount,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                textStyle = MaterialTheme.typography.displayLarge,
                placeholder = { Text("۰") },
                suffix = { Text(if (state.prefs.displayCurrency.name == "TOMAN") "تومان" else "ریال") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Text("دسته", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.categories.filter {
                    when (state.type) {
                        TxType.INCOME -> it.kind.name != "EXPENSE"
                        TxType.TRANSFER -> it.kind.name == "TRANSFER" || it.kind.name == "ANY"
                        else -> it.kind.name != "INCOME"
                    }
                }.forEach { c ->
                    FilterChip(selected = state.categoryId == c.id, onClick = { vm.setCategory(c.id) }, label = { Text(c.name) })
                }
            }
            Text("حساب", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.accounts.forEach { a ->
                    FilterChip(selected = state.accountId == a.id, onClick = { vm.setAccount(a.id) }, label = { Text(a.name) })
                }
            }
            Text(
                "تاریخ: ${JalaliDate.from(state.occurredAt).formatLong(state.prefs.persianDigits)}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("یادداشت") },
            )
            Button(
                onClick = vm::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره") }
            if (state.editingId != null) {
                Button(onClick = { state.editingId?.let(onCreateRule) }, modifier = Modifier.fillMaxWidth()) {
                    Text("ساخت قانون از این تراکنش")
                }
            }
        }
    }
}
