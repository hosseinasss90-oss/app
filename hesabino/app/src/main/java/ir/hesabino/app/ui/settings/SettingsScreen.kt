package ir.hesabino.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.sms.SmsImportWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onLock: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            val req = OneTimeWorkRequestBuilder<SmsImportWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(ctx).enqueue(req)
            scope.launch { snack.showSnackbar("اسکن پیامک‌ها شروع شد") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("تنظیمات") }) },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("نمایش", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("واحد پول: ${if (state.displayCurrency == DisplayCurrency.TOMAN) "تومان" else "ریال"}") },
                trailingContent = {
                    Switch(
                        checked = state.displayCurrency == DisplayCurrency.TOMAN,
                        onCheckedChange = { vm.setCurrency(if (it) DisplayCurrency.TOMAN else DisplayCurrency.RIAL) },
                    )
                },
            )
            ListItem(
                headlineContent = { Text("ارقام فارسی") },
                trailingContent = { Switch(checked = state.persianDigits, onCheckedChange = vm::setPersianDigits) },
            )
            ListItem(
                headlineContent = { Text("حالت خصوصی") },
                supportingContent = { Text("مخفی کردن مبالغ در صفحه") },
                trailingContent = { Switch(checked = state.privacyMode, onCheckedChange = vm::setPrivacy) },
            )

            Text("پول من", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            ListItem(headlineContent = { Text("حساب‌ها") }, modifier = androidx.compose.ui.Modifier, trailingContent = {
                Button(onClick = onAccounts) { Text("مدیریت") }
            })
            ListItem(headlineContent = { Text("دسته‌ها") }, trailingContent = {
                Button(onClick = onCategories) { Text("مدیریت") }
            })

            Text("پیامک بانکی", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            Text(
                "حسابینو پیامک‌ها را فقط روی همین دستگاه می‌خواند. چیزی به اینترنت نمی‌رود.",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ListItem(
                headlineContent = { Text("ذخیره متن خام پیامک") },
                supportingContent = { Text("پیش‌فرض خاموش — فقط برای دیباگ پارسر") },
                trailingContent = { Switch(checked = state.storeRawSms, onCheckedChange = vm::setStoreRaw) },
            )
            Button(
                onClick = {
                    val needed = buildList {
                        add(Manifest.permission.READ_SMS)
                        add(Manifest.permission.RECEIVE_SMS)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.filter {
                        ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needed.isEmpty()) {
                        val req = OneTimeWorkRequestBuilder<SmsImportWorker>().build()
                        WorkManager.getInstance(ctx).enqueue(req)
                        scope.launch { snack.showSnackbar("اسکن پیامک‌ها شروع شد") }
                    } else {
                        smsLauncher.launch(needed.toTypedArray())
                    }
                },
                modifier = Modifier.padding(16.dp),
            ) { Text("فعال‌سازی و اسکن پیامک‌های قبلی") }

            Text("داده", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            Button(onClick = { scope.launch { snack.showSnackbar(vm.exportHint()) } }, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("آماده‌سازی خروجی JSON / CSV")
            }
            Text(
                "خروجی از طریق اشتراک‌گذاری سیستم ذخیره می‌شود. بازگردانی JSON در نسخه بعد کامل می‌شود.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("امنیت", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("قفل اپ") },
                trailingContent = { Switch(checked = state.appLockEnabled, onCheckedChange = { if (it) onLock() else vm.setLock(false) }) },
            )
            ListItem(
                headlineContent = { Text("ورود با اثرانگشت") },
                trailingContent = { Switch(checked = state.biometricEnabled, onCheckedChange = vm::setBiometric) },
            )

            Text("درباره", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("حسابینو ۱.۰.۰") },
                supportingContent = { Text("پردازش کاملاً روی دستگاه · بدون حساب ابری") },
            )
        }
    }
}
