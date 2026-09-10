package ir.hesabino.app.data.repository

import ir.hesabino.app.data.local.db.HesabinoDatabase
import ir.hesabino.app.data.local.db.entity.AutomationRuleEntity
import ir.hesabino.app.data.local.db.entity.BankEventEntity
import ir.hesabino.app.data.local.db.entity.DraftTransactionEntity
import ir.hesabino.app.data.local.db.toDomain
import ir.hesabino.app.data.local.db.toEntity
import ir.hesabino.app.domain.model.AutomationRule
import ir.hesabino.app.domain.model.BankEvent
import ir.hesabino.app.domain.model.DraftTransaction
import ir.hesabino.app.domain.model.RuleAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepository @Inject constructor(
    private val db: HesabinoDatabase,
) {
    private val events = db.bankEvents()
    private val rules = db.rules()
    private val drafts = db.drafts()

    suspend fun insertEventIgnore(entity: BankEventEntity): Long = events.insertIgnore(entity)
    suspend fun eventByFingerprint(fp: String) = events.byFingerprint(fp)?.toDomain()
    suspend fun eventById(id: Long) = events.get(id)?.toDomain()
    suspend fun recentEvents(limit: Int = 20) = events.recent(limit).map { it.toDomain() }
    fun observeStoredEventCount(): Flow<Int> = events.observeCount()
    suspend fun storedCountNow(): Int = events.count()

    /** برای عیب‌یابی: شمار پیامک‌های ذخیره‌شده به تفکیک وضعیت پارس. */
    suspend fun parseStatusCounts(): Map<String, Int> {
        val statuses = listOf("PARSED", "PARTIAL", "UNPARSED")
        return statuses.associateWith { events.countByParseStatus(it) }
    }

    suspend fun storedSenders(): List<String> = events.distinctSenders()
    suspend fun recentBySender(sender: String, limit: Int = 20) =
        events.recentBySender(sender, limit).map { it.toDomain() }

    fun observeRules(): Flow<List<AutomationRule>> = rules.observeAll().map { list ->
        val conds = rules.allConditions().groupBy { it.ruleId }
        val acts = rules.allActions().associateBy { it.ruleId }
        list.map { it.toDomain(conds[it.id].orEmpty(), acts[it.id]) }
    }

    suspend fun loadRules(): List<AutomationRule> {
        val list = rules.all()
        val conds = rules.allConditions().groupBy { it.ruleId }
        val acts = rules.allActions().associateBy { it.ruleId }
        return list.map { it.toDomain(conds[it.id].orEmpty(), acts[it.id]) }
    }

    suspend fun saveRule(rule: AutomationRule): Long {
        val now = System.currentTimeMillis()
        val entity = AutomationRuleEntity(
            id = rule.id,
            name = rule.name,
            isEnabled = rule.isEnabled,
            autoApprove = rule.autoApprove,
            priority = rule.priority,
            createdAt = now,
            updatedAt = now,
        )
        val action = rule.action ?: RuleAction(title = rule.name)
        return rules.saveFull(
            rule = entity,
            conditions = rule.conditions.map { it.toEntity(rule.id) },
            action = action.toEntity(rule.id),
            isNew = rule.id == 0L,
        )
    }

    suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        val e = rules.get(id) ?: return
        rules.update(e.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRule(id: Long) = rules.delete(id)

    fun observePendingDrafts(): Flow<List<DraftTransaction>> =
        drafts.observePending().map { it.map { e -> e.toDomain() } }

    fun observePendingCount(): Flow<Int> = drafts.observePendingCount()

    suspend fun draftByEvent(eventId: Long) = drafts.byEvent(eventId)?.toDomain()

    suspend fun insertDraft(d: DraftTransaction): Long {
        val now = System.currentTimeMillis()
        return drafts.insertIgnore(
            DraftTransactionEntity(
                id = 0,
                bankEventId = d.bankEventId,
                ruleId = d.ruleId,
                suggestedTitle = d.suggestedTitle,
                suggestedType = d.suggestedType.name,
                suggestedAmountRials = d.suggestedAmountRials,
                suggestedCategoryId = d.suggestedCategoryId,
                suggestedAccountId = d.suggestedAccountId,
                suggestedNote = d.suggestedNote,
                status = d.status.name,
                createdAt = now,
                resolvedAt = null,
            ),
        )
    }

    suspend fun resolveDraft(id: Long, approved: Boolean) {
        drafts.setStatus(id, if (approved) "APPROVED" else "REJECTED", System.currentTimeMillis())
    }

    suspend fun getDraft(id: Long) = drafts.get(id)?.toDomain()
}
