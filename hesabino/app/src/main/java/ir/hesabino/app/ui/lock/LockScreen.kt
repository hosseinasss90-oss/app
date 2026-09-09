package ir.hesabino.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.util.toPersianDigits
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    prefs: UserPreferences,
    setup: Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    Column(
        Modifier.fillMaxSize(),
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
        Spacer(Modifier.height(24.dp))
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
