package ir.hesabino.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.repository.AutomationRepository
import ir.hesabino.app.ui.accounts.AccountsScreen
import ir.hesabino.app.ui.addedit.AddEditTransactionScreen
import ir.hesabino.app.ui.automations.AutomationsScreen
import ir.hesabino.app.ui.categories.CategoriesScreen
import ir.hesabino.app.ui.dashboard.DashboardScreen
import ir.hesabino.app.ui.rules.RuleBuilderScreen
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.ui.lock.LockScreen
import ir.hesabino.app.ui.settings.SettingsScreen
import ir.hesabino.app.ui.transactions.TransactionsScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "خانه", Icons.Outlined.Home),
    Tab("tx", "تراکنش‌ها", Icons.Outlined.List),
    Tab("auto", "خودکار", Icons.Outlined.AutoAwesome),
    Tab("settings", "تنظیمات", Icons.Outlined.Settings),
)

@HiltViewModel
class NavViewModel @Inject constructor(
    automation: AutomationRepository,
    val userPrefs: UserPreferences,
) : ViewModel() {
    val draftCount = automation.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun HesabinoNav() {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route
    val showBar = route in tabs.map { it.route }
    val navVm: NavViewModel = hiltViewModel()
    val drafts by navVm.draftCount.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab.route == "auto" && drafts > 0) {
                                    BadgedBox(badge = { Badge { Text("$drafts") } }) {
                                        Icon(tab.icon, tab.label)
                                    }
                                } else Icon(tab.icon, tab.label)
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                DashboardScreen(
                    onAdd = { nav.navigate("add/0") },
                    onSeeAll = {
                        nav.navigate("tx") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTx = { nav.navigate("add/$it") },
                )
            }
            composable("tx") {
                TransactionsScreen(
                    onAdd = { nav.navigate("add/0") },
                    onTx = { nav.navigate("add/$it") },
                )
            }
            composable("auto") {
                AutomationsScreen(
                    onNewRule = { nav.navigate("rule/0/0/0") },
                    onEditRule = { nav.navigate("rule/$it/0/0") },
                    onEditDraft = { nav.navigate("rule/0/0/$it") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    onAccounts = { nav.navigate("accounts") },
                    onCategories = { nav.navigate("categories") },
                    onLock = { nav.navigate("setupLock") },
                )
            }
            composable(
                "add/{txId}",
                arguments = listOf(navArgument("txId") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                AddEditTransactionScreen(
                    onDone = { nav.popBackStack() },
                    onCreateRule = { nav.navigate("rule/0/$it/0") },
                )
            }
            composable(
                "rule/{ruleId}/{sampleTxId}/{sampleDraftId}",
                arguments = listOf(
                    navArgument("ruleId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("sampleTxId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("sampleDraftId") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) {
                RuleBuilderScreen(onDone = { nav.popBackStack() })
            }
            composable("accounts") { AccountsScreen(onBack = { nav.popBackStack() }) }
            composable("categories") { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable("setupLock") {
                LockScreen(prefs = navVm.userPrefs, setup = true, onUnlocked = { nav.popBackStack() })
            }
        }
    }
}
