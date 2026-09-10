package ir.hesabino.app.ui.categories

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.CategoryKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** دسته‌بندی‌های پیشنهادی برای افزودن سریع. */
data class SuggestedCategory(val name: String, val kind: CategoryKind, val color: Color)

private val Suggestions = listOf(
    SuggestedCategory("خوراک", CategoryKind.EXPENSE, Color(0xFFE8A33B)),
    SuggestedCategory("سوپرمارکت", CategoryKind.EXPENSE, Color(0xFF2F8F6B)),
    SuggestedCategory("حمل‌ونقل", CategoryKind.EXPENSE, Color(0xFF3B82A8)),
    SuggestedCategory("بنزین", CategoryKind.EXPENSE, Color(0xFFC45B4A)),
    SuggestedCategory("قبوض", CategoryKind.EXPENSE, Color(0xFF7C6BD6)),
    SuggestedCategory("مسکن", CategoryKind.EXPENSE, Color(0xFF3E6375)),
    SuggestedCategory("سلامت", CategoryKind.EXPENSE, Color(0xFFD65C5C)),
    SuggestedCategory("اینترنت", CategoryKind.EXPENSE, Color(0xFF2F6F8F)),
    SuggestedCategory("سرگرمی", CategoryKind.EXPENSE, Color(0xFF9A5B8E)),
    SuggestedCategory("پوشاک", CategoryKind.EXPENSE, Color(0xFFC77B2F)),
    SuggestedCategory("حقوق", CategoryKind.INCOME, Color(0xFF1F8A5B)),
    SuggestedCategory("هدیه", CategoryKind.INCOME, Color(0xFFE8A33B)),
    SuggestedCategory("سود", CategoryKind.INCOME, Color(0xFF2F8F6B)),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val finance: FinanceRepository,
) : ViewModel() {
    private val kindFilter = MutableStateFlow<CategoryKind?>(null)

    val items = combine(finance.observeCategories(), kindFilter) { cats, kind ->
        if (kind == null) cats else cats.filter { it.kind == kind }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(kind: CategoryKind?) { kindFilter.value = kind }

    fun add(name: String, kind: CategoryKind) {
        viewModelScope.launch {
            finance.addCategory(Category(name = name.trim(), iconKey = name.trim(), kind = kind))
        }
    }

    fun archive(id: Long) { viewModelScope.launch { finance.archiveCategory(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, vm: CategoriesViewModel = hiltViewModel()) {
    val list by vm.items.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    var showAdd by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<CategoryKind?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دسته‌بندی‌ها") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowForward, "بازگشت") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Text(
                    "دسته‌بندی‌ها به‌خوبی مخارج و درآمدهای شما را مرتب نگه می‌دارند و نمودارها را معنادار می‌کنند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null; vm.setFilter(null) }, label = { Text("همه") })
                    FilterChip(selected = filter == CategoryKind.EXPENSE, onClick = { filter = CategoryKind.EXPENSE; vm.setFilter(CategoryKind.EXPENSE) }, label = { Text("هزینه") })
                    FilterChip(selected = filter == CategoryKind.INCOME, onClick = { filter = CategoryKind.INCOME; vm.setFilter(CategoryKind.INCOME) }, label = { Text("درآمد") })
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "دسته‌ای نیست. از پیشنهادهای زیر یکی را بیفزایید یا دستهٔ دلخواه بسازید.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(list, key = { it.id }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dot = Suggestions.firstOrNull { it.name == c.name }?.color
                        ?: if (c.kind == CategoryKind.INCOME) Color(0xFF1F8A5B) else Color(0xFF2F6F8F)
                    Box(Modifier.size(36.dp).background(dot.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(c.name.take(1), fontWeight = FontWeight.Bold, color = dot)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (c.kind) { CategoryKind.EXPENSE -> "هزینه"; CategoryKind.INCOME -> "درآمد"; else -> "عمومی" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { vm.archive(c.id) }) { Text("آرشیو") }
                }
            }

            item {
                Text(
                    "افزودن سریع",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp),
                )
            }
            items(Suggestions.filter { filter == null || it.kind == filter }) { s ->
                val exists = list.any { it.name == s.name }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = false,
                        onClick = { if (!exists) vm.add(s.name, s.kind) },
                        enabled = !exists,
                        label = { Text(s.name) },
                        leadingIcon = { Box(Modifier.size(14.dp).background(s.color, CircleShape)) {} },
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showAdd = !showAdd }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, null)
                        Text(if (showAdd) "بستن فرم دستی" else "ساخت دستهٔ دلخواه")
                    }
                }
            }
            if (showAdd) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام دسته") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = kind == CategoryKind.EXPENSE, onClick = { kind = CategoryKind.EXPENSE }, label = { Text("هزینه") })
                            FilterChip(selected = kind == CategoryKind.INCOME, onClick = { kind = CategoryKind.INCOME }, label = { Text("درآمد") })
                        }
                        Button(
                            onClick = { if (name.isNotBlank()) { vm.add(name, kind); name = "" } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("افزودن") }
                    }
                }
            }
        }
    }
}
