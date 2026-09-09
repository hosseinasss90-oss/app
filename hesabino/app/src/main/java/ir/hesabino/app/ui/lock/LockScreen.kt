package ir.hesabino.app.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.util.toPersianDigits
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun LockScreen(
    prefs: UserPreferences,
    setup: Boolean,
    onUnlocked: () -> Unit,
    biometricEnabled: Boolean = false,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun submit(next: String) {
        pin = next
        if (next.length < 6) return
        scope.launch {
            if (setup) {
                prefs.setPin(next)
                onUnlocked()
            } else if (prefs.verifyPin(next)) {
                onUnlocked()
            } else {
                error = "پین نادرست"
                pin = ""
            }
        }
    }

    fun launchBiometric() {
        val act = activity ?: return
        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt(
            act,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                }
                override fun onAuthenticationFailed() {
                    error = "اثر انگشت شناسایی نشد؛ دوباره تلاش کنید"
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود با اثر انگشت")
            .setSubtitle("اثر انگشت خود را برای ورود به حسابینو لمس کنید")
            .setNegativeButtonText("استفاده از پین")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("حسابینو", style = MaterialTheme.typography.headlineMedium)
        Text(if (setup) "یک پین ۶ رقمی بسازید" else "پین را وارد کنید", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            List(6) { i -> if (i < pin.length) "●" else "○" }.joinToString("  "),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))

        // ورود با اثر انگشت (فقط وقتی فعال و دستگاه سازگار باشد)
        if (!setup && biometricEnabled && activity != null && canUseBiometric(context)) {
            OutlinedButton(onClick = ::launchBiometric, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Fingerprint, null)
                Spacer(Modifier.size(8.dp))
                Text("ورود با اثر انگشت")
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(8.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = {
                            when (key) {
                                "⌫" -> pin = pin.dropLast(1)
                                "✓" -> if (pin.length == 6) submit(pin)
                                else -> if (pin.length < 6) submit(pin + key)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                    ) { Text(if (key.all { it.isDigit() }) key.toPersianDigits() else key, style = MaterialTheme.typography.titleLarge) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** بررسی وجود سخت‌افزار و اثر انگشت ثبت‌شده. */
@Suppress("DEPRECATION")
fun canUseBiometric(context: android.content.Context): Boolean {
    return try {
        val res = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        res == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) {
        false
    }
}
