package ir.hesabino.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.backup.BackupRepository
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.domain.model.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val backup: BackupRepository,
) : ViewModel() {

    val state = prefs.prefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPrefs())

    fun setCurrency(v: DisplayCurrency) { viewModelScope.launch { prefs.setDisplayCurrency(v) } }
    fun setPersianDigits(v: Boolean) { viewModelScope.launch { prefs.setPersianDigits(v) } }
    fun setPrivacy(v: Boolean) { viewModelScope.launch { prefs.setPrivacyMode(v) } }
    fun setStoreRaw(v: Boolean) { viewModelScope.launch { prefs.setStoreRawSms(v) } }
    fun setLock(v: Boolean) { viewModelScope.launch { prefs.setLock(v) } }
    fun setBiometric(v: Boolean) { viewModelScope.launch { prefs.setBiometric(v) } }

    suspend fun exportHint(): String {
        val json = backup.exportJson()
        val csv = backup.exportCsv(state.value.displayCurrency, state.value.persianDigits)
        return "JSON ${json.length} کاراکتر · CSV ${csv.lineSequence().count()} خط — از اشتراک‌گذاری سیستم استفاده کنید"
    }
}
