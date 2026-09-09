package ir.hesabino.app.sms

import android.content.Context
import android.provider.Telephony
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsImportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ingestor: SmsIngestor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cr = applicationContext.contentResolver
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        cr.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { c ->
            val iAddr = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndex(Telephony.Sms.BODY)
            val iDate = c.getColumnIndex(Telephony.Sms.DATE)
            var n = 0
            while (c.moveToNext() && n < MAX) {
                val sender = c.getString(iAddr).orEmpty()
                val body = c.getString(iBody).orEmpty()
                val date = c.getLong(iDate)
                if (sender.isNotBlank() && body.isNotBlank()) {
                    ingestor.ingest(sender, body, date)
                    n++
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE = "sms-initial-import"
        private const val MAX = 400
    }
}
