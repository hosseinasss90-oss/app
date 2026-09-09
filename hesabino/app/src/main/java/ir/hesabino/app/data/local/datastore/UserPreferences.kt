package ir.hesabino.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.domain.model.UnmatchedPolicy
import ir.hesabino.app.domain.model.UserPrefs
import ir.hesabino.app.engine.fingerprint.Fingerprint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hesabino_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val displayCurrency = stringPreferencesKey("display_currency")
        val persianDigits = booleanPreferencesKey("persian_digits")
        val privacyMode = booleanPreferencesKey("privacy_mode")
        val storeRawSms = booleanPreferencesKey("store_raw_sms")
        val unmatched = stringPreferencesKey("unmatched_policy")
        val lock = booleanPreferencesKey("app_lock")
        val biometric = booleanPreferencesKey("biometric")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val defaultAccount = longPreferencesKey("default_account")
        val lastExpenseCat = longPreferencesKey("last_expense_cat")
        val lastIncomeCat = longPreferencesKey("last_income_cat")
        val watchedSenders = stringSetPreferencesKey("watched_senders")
        val debug = booleanPreferencesKey("debug_log")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            displayCurrency = runCatching { DisplayCurrency.valueOf(p[Keys.displayCurrency] ?: "TOMAN") }.getOrDefault(DisplayCurrency.TOMAN),
            persianDigits = p[Keys.persianDigits] ?: true,
            privacyMode = p[Keys.privacyMode] ?: false,
            storeRawSms = p[Keys.storeRawSms] ?: false,
            unmatchedPolicy = runCatching { UnmatchedPolicy.valueOf(p[Keys.unmatched] ?: "DRAFT") }.getOrDefault(UnmatchedPolicy.DRAFT),
            appLockEnabled = p[Keys.lock] ?: false,
            biometricEnabled = p[Keys.biometric] ?: false,
            onboardingDone = p[Keys.onboarding] ?: false,
            defaultAccountId = p[Keys.defaultAccount],
            lastExpenseCategoryId = p[Keys.lastExpenseCat],
            lastIncomeCategoryId = p[Keys.lastIncomeCat],
            watchedSenders = p[Keys.watchedSenders]?.filter { it.isNotBlank() }?.sorted().orEmpty(),
            debugLogEnabled = p[Keys.debug] ?: false,
        )
    }

    suspend fun setDisplayCurrency(v: DisplayCurrency) = edit { it[Keys.displayCurrency] = v.name }
    suspend fun setPersianDigits(v: Boolean) = edit { it[Keys.persianDigits] = v }
    suspend fun setPrivacyMode(v: Boolean) = edit { it[Keys.privacyMode] = v }
    suspend fun setStoreRawSms(v: Boolean) = edit { it[Keys.storeRawSms] = v }
    suspend fun setUnmatched(v: UnmatchedPolicy) = edit { it[Keys.unmatched] = v.name }
    suspend fun setLock(v: Boolean) = edit { it[Keys.lock] = v }
    suspend fun setBiometric(v: Boolean) = edit { it[Keys.biometric] = v }
    suspend fun setOnboardingDone() = edit { it[Keys.onboarding] = true }
    suspend fun setDefaultAccount(id: Long?) = edit {
        if (id == null) it.remove(Keys.defaultAccount) else it[Keys.defaultAccount] = id
    }
    suspend fun setLastCategory(expense: Boolean, id: Long) = edit {
        if (expense) it[Keys.lastExpenseCat] = id else it[Keys.lastIncomeCat] = id
    }
    suspend fun setDebug(v: Boolean) = edit { it[Keys.debug] = v }

    suspend fun addWatchedSender(sender: String) {
        val s = sender.trim()
        if (s.isBlank()) return
        edit { prefs ->
            val cur = prefs[Keys.watchedSenders].orEmpty()
            prefs[Keys.watchedSenders] = cur + s
        }
    }

    suspend fun removeWatchedSender(sender: String) {
        edit { prefs ->
            val cur = prefs[Keys.watchedSenders].orEmpty()
            prefs[Keys.watchedSenders] = cur - sender
        }
    }

    suspend fun isSenderWatched(sender: String): Boolean =
        context.dataStore.data.first()[Keys.watchedSenders]?.contains(sender) == true

    suspend fun setPin(pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        val hash = Fingerprint.sha256(salt + pin)
        edit {
            it[Keys.pinSalt] = salt
            it[Keys.pinHash] = hash
            it[Keys.lock] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val p = context.dataStore.data.first()
        val salt = p[Keys.pinSalt].orEmpty()
        val hash = p[Keys.pinHash].orEmpty()
        return hash.isNotEmpty() && Fingerprint.sha256(salt + pin) == hash
    }

    private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
