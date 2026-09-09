package ir.hesabino.app.ui.categories

import androidx.compose.foundation.layout.Column
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
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.CategoryKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val finance: FinanceRepository,
) : ViewModel() {
    val items = finance.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun add(name: String) {
        viewModelScope.launch {
            finance.addCategory(Category(name = name, iconKey = "other", kind = CategoryKind.EXPENSE))
        }
    }
    fun archive(id: Long) { viewModelScope.launch { finance.archiveCategory(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, vm: CategoriesViewModel = hiltViewModel()) {
    val list by vm.items.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دسته‌ها") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, null) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(list, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text(c.kind.name) },
                    trailingContent = { Button(onClick = { vm.archive(c.id) }) { Text("آرشیو") } },
                )
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("دسته جدید") })
                    Button(onClick = { if (name.isNotBlank()) { vm.add(name); name = "" } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("افزودن") }
                }
            }
        }
    }
}
