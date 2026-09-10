package ir.hesabino.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.sms.SmsImportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HesabinoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var finance: FinanceRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch { finance.seedIfEmpty() }
        // اگر مجوز خواندن پیامک داده شده باشد، هر بار که اپ باز می‌شود پیامک‌های
        // بانکی تازه را خودکار اسکن می‌کند تا کاربر ناچار نباشد دکمهٔ دستی را بزند.
        scheduleInitialImport()
    }

    private fun scheduleInitialImport() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val req = OneTimeWorkRequestBuilder<SmsImportWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            SmsImportWorker.UNIQUE,
            ExistingWorkPolicy.KEEP,
            req,
        )
    }
}
