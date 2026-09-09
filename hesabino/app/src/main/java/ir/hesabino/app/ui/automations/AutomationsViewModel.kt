package ir.hesabino.app.ui.automations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.repository.AutomationRepository
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.CreatedFrom
import ir.hesabino.app.domain.model.DraftTransaction
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AutomationsState(
    val prefs: UserPrefs = UserPrefs(),
    val drafts: List<DraftTransaction> = emptyList(),
    val rules: List<AutomationRule> = emptyList(),
)

@HiltViewModel
class AutomationsViewModel @Inject constructor(
    private val automation: AutomationRepository,
    private val finance: FinanceRepository,
    prefs: UserPreferences,
) : ViewModel() {

    val state = combine(
        prefs.prefs,
        automation.observePendingDrafts(),
        automation.observeRules(),
    ) { p, d, r -> AutomationsState(p, d, r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutomationsState())

    fun toggle(id: Long, enabled: Boolean) {
        viewModelScope.launch { automation.setRuleEnabled(id, enabled) }
    }

    fun reject(id: Long) {
        viewModelScope.launch { automation.resolveDraft(id, approved = false) }
    }

    fun approve(id: Long) {
        viewModelScope.launch {
            val d = automation.getDraft(id) ?: return@launch
            val event = automation.eventById(d.bankEventId)
            finance.upsertTransaction(
                Transaction(
                    type = d.suggestedType,
                    amountRials = d.suggestedAmountRials,
                    occurredAt = event?.receivedAt ?: System.currentTimeMillis(),
                    accountId = d.suggestedAccountId ?: 1L,
                    categoryId = d.suggestedCategoryId,
                    title = d.suggestedTitle,
                    note = d.suggestedNote,
                    createdFrom = CreatedFrom.SMS_DRAFT,
                    sourceEventId = d.bankEventId,
                ),
            )
            automation.resolveDraft(id, approved = true)
        }
    }
}
