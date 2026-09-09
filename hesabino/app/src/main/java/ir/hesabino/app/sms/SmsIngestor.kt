package ir.hesabino.app.sms

import ir.hesabino.app.data.local.datastore.UserPreferences
import ir.hesabino.app.data.local.db.entity.BankEventEntity
import ir.hesabino.app.data.repository.AutomationRepository
import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.CreatedFrom
import ir.hesabino.app.domain.model.DraftStatus
import ir.hesabino.app.domain.model.DraftTransaction
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType
import ir.hesabino.app.domain.model.UnmatchedPolicy
import ir.hesabino.app.engine.fingerprint.Fingerprint
import ir.hesabino.app.engine.parser.ParserRegistry
import ir.hesabino.app.engine.rules.RuleEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class IngestOutcome(
    val duplicate: Boolean,
    val eventId: Long?,
    val transactionId: Long?,
    val draftId: Long?,
    val unparsed: Boolean,
)

@Singleton
class SmsIngestor @Inject constructor(
    private val automation: AutomationRepository,
    private val finance: FinanceRepository,
    private val prefs: UserPreferences,
    private val registry: ParserRegistry,
) {
    suspend fun ingest(senderId: String, body: String, receivedAt: Long): IngestOutcome {
        val parsed = registry.parse(senderId, body, receivedAt)
        val amount = parsed?.amountRials
        val last4 = parsed?.cardLast4
        val fp = Fingerprint.compute(senderId, amount, last4, receivedAt, body)
        val existing = automation.eventByFingerprint(fp)
        if (existing != null) {
            return IngestOutcome(duplicate = true, eventId = existing.id, transactionId = null, draftId = null, unparsed = false)
        }
        val storeRaw = prefs.prefs.first().storeRawSms
        val now = System.currentTimeMillis()
        val entity = BankEventEntity(
            senderId = senderId,
            bankKey = parsed?.bankKey,
            parsedType = parsed?.type?.name ?: "UNKNOWN",
            amountRials = amount,
            currencyHint = parsed?.unit?.name ?: "UNKNOWN",
            cardLast4 = last4,
            balanceRials = parsed?.balanceRials,
            description = parsed?.description,
            receivedAt = receivedAt,
            fingerprint = fp,
            bodyHash = Fingerprint.bodyHash(body),
            rawBody = if (storeRaw) body else null,
            parseStatus = parsed?.status?.name ?: "UNPARSED",
            createdAt = now,
        )
        val inserted = automation.insertEventIgnore(entity)
        if (inserted == -1L) {
            val again = automation.eventByFingerprint(fp)
            return IngestOutcome(true, again?.id, null, null, false)
        }
        val event = automation.eventById(inserted) ?: return IngestOutcome(false, inserted, null, null, true)
        if (finance.bySourceEvent(inserted) != null) {
            return IngestOutcome(false, inserted, null, null, false)
        }
        if (automation.draftByEvent(inserted) != null) {
            return IngestOutcome(false, inserted, null, null, false)
        }

        val user = prefs.prefs.first()
        val rules = automation.loadRules()
        val decision = RuleEngine.decide(event, rules)

        val winner = decision.winner
        if (winner != null && winner.action != null && event.amountRials != null) {
            val action = winner.action
            val type = when (event.parsedType.name) {
                "CREDIT" -> TxType.INCOME
                else -> TxType.EXPENSE
            }
            if (winner.autoApprove) {
                val tx = Transaction(
                    type = type,
                    amountRials = event.amountRials,
                    occurredAt = event.receivedAt,
                    accountId = action.accountId ?: user.defaultAccountId ?: 1L,
                    categoryId = action.categoryId,
                    title = action.title,
                    note = action.noteTemplate,
                    tags = action.tags,
                    createdFrom = CreatedFrom.SMS_RULE,
                    sourceEventId = event.id,
                )
                val txId = finance.upsertTransaction(tx)
                return IngestOutcome(false, event.id, txId, null, false)
            } else {
                val draftId = automation.insertDraft(
                    DraftTransaction(
                        bankEventId = event.id,
                        ruleId = winner.id,
                        suggestedTitle = action.title,
                        suggestedType = type,
                        suggestedAmountRials = event.amountRials,
                        suggestedCategoryId = action.categoryId,
                        suggestedAccountId = action.accountId ?: user.defaultAccountId,
                        suggestedNote = action.noteTemplate,
                        status = DraftStatus.PENDING,
                    ),
                )
                return IngestOutcome(false, event.id, null, draftId, false)
            }
        }

        if (user.unmatchedPolicy == UnmatchedPolicy.IGNORE || event.amountRials == null) {
            return IngestOutcome(false, event.id, null, null, parsed == null)
        }
        val type = when (event.parsedType.name) {
            "CREDIT" -> TxType.INCOME
            else -> TxType.EXPENSE
        }
        val draftId = automation.insertDraft(
            DraftTransaction(
                bankEventId = event.id,
                ruleId = null,
                suggestedTitle = event.description?.take(40) ?: "تراکنش بانکی",
                suggestedType = type,
                suggestedAmountRials = event.amountRials,
                suggestedCategoryId = null,
                suggestedAccountId = user.defaultAccountId,
                suggestedNote = if (decision.tied) "چند قانون هم‌اولویت" else null,
                status = DraftStatus.PENDING,
            ),
        )
        return IngestOutcome(false, event.id, null, draftId, parsed?.status?.name == "UNPARSED")
    }
}
