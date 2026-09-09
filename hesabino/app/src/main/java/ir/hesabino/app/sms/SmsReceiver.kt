package ir.hesabino.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var ingestor: SmsIngestor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                val grouped = messages.groupBy { it.originatingAddress.orEmpty() }
                grouped.forEach { (sender, parts) ->
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }
                    val receivedAt = parts.minOf { it.timestampMillis }
                    if (sender.isNotBlank() && body.isNotBlank()) {
                        ingestor.ingest(sender, body, receivedAt)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
