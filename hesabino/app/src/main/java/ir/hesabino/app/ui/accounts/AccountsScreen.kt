package ir.hesabino.app.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Account
import ir.hesabino.app.domain.model.AccountType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val finance: FinanceRepository,
) : ViewModel() {
    val accounts = finance.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun add(name: String, last4: String) {
        viewModelScope.launch {
            finance.addAccount(Account(name = name, type = if (last4.isBlank()) AccountType.CASH else AccountType.CARD, last4 = last4.ifBlank { null }))
        }
    }
    fun archive(id: Long) { viewModelScope.launch { finance.archiveAccount(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onBack: () -> Unit, vm: AccountsViewModel = hiltViewModel()) {
    val list by vm.accounts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حساب‌ها") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "بازگشت") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list, key = { it.id }) { a ->
                ListItem(
                    headlineContent = { Text(a.name) },
                    supportingContent = { Text(buildString {
                        append(a.type.name)
                        if (!a.last4.isNullOrBlank()) append(" · *${a.last4}")
                    }) },
                    trailingContent = { Button(onClick = { vm.archive(a.id) }) { Text("آرشیو") } },
                )
            }
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حساب جدید")
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام") })
                    OutlinedTextField(last4, { last4 = it.filter { ch -> ch.isDigit() }.take(4) }, modifier = Modifier.fillMaxWidth(), label = { Text("۴ رقم آخر (اختیاری)") })
                    Button(onClick = { if (name.isNotBlank()) { vm.add(name, last4); name = ""; last4 = "" } }, modifier = Modifier.fillMaxWidth()) { Text("افزودن") }
                }
            }
        }
    }
}
