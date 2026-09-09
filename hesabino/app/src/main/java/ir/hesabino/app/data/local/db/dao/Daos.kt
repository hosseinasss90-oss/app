package ir.hesabino.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ir.hesabino.app.data.local.db.entity.AccountEntity
import ir.hesabino.app.data.local.db.entity.AutomationRuleEntity
import ir.hesabino.app.data.local.db.entity.BankEventEntity
import ir.hesabino.app.data.local.db.entity.CategoryEntity
import ir.hesabino.app.data.local.db.entity.DraftTransactionEntity
import ir.hesabino.app.data.local.db.entity.RuleActionEntity
import ir.hesabino.app.data.local.db.entity.RuleConditionEntity
import ir.hesabino.app.data.local.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: Long): AccountEntity?

    @Insert
    suspend fun insert(entity: AccountEntity): Long

    @Update
    suspend fun update(entity: AccountEntity)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Insert
    suspend fun insertAll(items: List<CategoryEntity>)

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE transactions SET categoryId = :intoId WHERE categoryId = :fromId")
    suspend fun reassignTransactions(fromId: Long, intoId: Long)

    @Transaction
    suspend fun merge(fromId: Long, intoId: Long) {
        reassignTransactions(fromId, intoId)
        archive(fromId)
    }
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE isDeleted = 0
          AND (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND occurredAt BETWEEN :from AND :to
          AND (:q = '' OR title LIKE '%' || :q || '%' OR note LIKE '%' || :q || '%')
        ORDER BY occurredAt DESC, id DESC
        """,
    )
    fun observeFiltered(
        from: Long,
        to: Long,
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        q: String,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE isDeleted = 0 AND occurredAt BETWEEN :from AND :to
        ORDER BY occurredAt DESC
        """,
    )
    fun observeRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE sourceEventId = :eventId AND isDeleted = 0 LIMIT 1")
    suspend fun bySourceEvent(eventId: Long): TransactionEntity?

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("UPDATE transactions SET isDeleted = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY occurredAt DESC")
    suspend fun allActive(): List<TransactionEntity>
}

@Dao
interface BankEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: BankEventEntity): Long

    @Query("SELECT * FROM bank_events WHERE fingerprint = :fp LIMIT 1")
    suspend fun byFingerprint(fp: String): BankEventEntity?

    @Query("SELECT * FROM bank_events WHERE id = :id")
    suspend fun get(id: Long): BankEventEntity?

    @Query("SELECT * FROM bank_events ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<BankEventEntity>

    @Query("SELECT * FROM bank_events WHERE senderId = :sender ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun recentBySender(sender: String, limit: Int): List<BankEventEntity>

    @Query("SELECT * FROM bank_events ORDER BY receivedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BankEventEntity>>
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM automation_rules ORDER BY priority ASC, id ASC")
    fun observeAll(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules ORDER BY priority ASC, id ASC")
    suspend fun all(): List<AutomationRuleEntity>

    @Query("SELECT * FROM automation_rules WHERE id = :id")
    suspend fun get(id: Long): AutomationRuleEntity?

    @Insert
    suspend fun insert(entity: AutomationRuleEntity): Long

    @Update
    suspend fun update(entity: AutomationRuleEntity)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM rule_conditions WHERE ruleId = :ruleId")
    suspend fun conditions(ruleId: Long): List<RuleConditionEntity>

    @Query("SELECT * FROM rule_conditions")
    suspend fun allConditions(): List<RuleConditionEntity>

    @Query("SELECT * FROM rule_actions WHERE ruleId = :ruleId")
    suspend fun action(ruleId: Long): RuleActionEntity?

    @Query("SELECT * FROM rule_actions")
    suspend fun allActions(): List<RuleActionEntity>

    @Insert
    suspend fun insertCondition(entity: RuleConditionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAction(entity: RuleActionEntity)

    @Query("DELETE FROM rule_conditions WHERE ruleId = :ruleId")
    suspend fun clearConditions(ruleId: Long)

    @Transaction
    suspend fun saveFull(
        rule: AutomationRuleEntity,
        conditions: List<RuleConditionEntity>,
        action: RuleActionEntity,
        isNew: Boolean,
    ): Long {
        val id = if (isNew) insert(rule) else {
            update(rule)
            rule.id
        }
        clearConditions(id)
        conditions.forEach { insertCondition(it.copy(ruleId = id)) }
        upsertAction(action.copy(ruleId = id))
        return id
    }
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft_transactions WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun observePending(): Flow<List<DraftTransactionEntity>>

    @Query("SELECT COUNT(*) FROM draft_transactions WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM draft_transactions WHERE bankEventId = :eventId LIMIT 1")
    suspend fun byEvent(eventId: Long): DraftTransactionEntity?

    @Query("SELECT * FROM draft_transactions WHERE id = :id")
    suspend fun get(id: Long): DraftTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: DraftTransactionEntity): Long

    @Update
    suspend fun update(entity: DraftTransactionEntity)

    @Query(
        """
        UPDATE draft_transactions
        SET status = :status, resolvedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun setStatus(id: Long, status: String, now: Long)
}
