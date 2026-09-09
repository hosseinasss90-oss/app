package ir.hesabino.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.ui.lock.LockScreen
import ir.hesabino.app.ui.navigation.HesabinoNav
import ir.hesabino.app.ui.theme.HesabinoTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HesabinoTheme {
                Surface(Modifier.fillMaxSize()) {
                    val prefs by userPrefs.prefs.collectAsStateWithLifecycle(initialValue = null)
                    var unlocked by remember { mutableStateOf(false) }
                    val current = prefs
                    when {
                        current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        current.appLockEnabled && !unlocked -> LockScreen(
                            prefs = userPrefs,
                            setup = false,
                            onUnlocked = { unlocked = true },
                        )
                        else -> HesabinoNav()
                    }
                }
            }
        }
    }
}
