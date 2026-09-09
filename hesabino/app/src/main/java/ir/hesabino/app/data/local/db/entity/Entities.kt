package ir.hesabino.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val last4: String?,
    val colorArgb: Int,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val kind: String,
    val parentId: Long?,
    val sortOrder: Int,
    val isArchived: Boolean,
)

@Entity(
    tableName = "bank_events",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index("senderId"),
        Index("receivedAt"),
    ],
)
data class BankEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val bankKey: String?,
    val parsedType: String,
    val amountRials: Long?,
    val currencyHint: String,
    val cardLast4: String?,
    val balanceRials: Long?,
    val description: String?,
    val receivedAt: Long,
    val fingerprint: String,
    val bodyHash: String,
    val rawBody: String?,
    val parseStatus: String,
    val createdAt: Long,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = BankEventEntity::class, parentColumns = ["id"], childColumns = ["sourceEventId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("occurredAt"),
        Index("accountId"),
        Index("categoryId"),
        Index("sourceEventId"),
        Index("isDeleted"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountRials: Long,
    val occurredAt: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val title: String,
    val note: String?,
    val tagsJson: String,
    val createdFrom: String,
    val sourceEventId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isEnabled: Boolean,
    val autoApprove: Boolean,
    val priority: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "rule_conditions",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("ruleId")],
)
data class RuleConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val field: String,
    val operator: String,
    val value: String,
)

@Entity(
    tableName = "rule_actions",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RuleActionEntity(
    @PrimaryKey val ruleId: Long,
    val title: String,
    val categoryId: Long?,
    val accountId: Long?,
    val tagsJson: String,
    val noteTemplate: String?,
)

@Entity(
    tableName = "draft_transactions",
    foreignKeys = [
        ForeignKey(entity = BankEventEntity::class, parentColumns = ["id"], childColumns = ["bankEventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AutomationRuleEntity::class, parentColumns = ["id"], childColumns = ["ruleId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index(value = ["bankEventId"], unique = true),
        Index("status"),
        Index("ruleId"),
    ],
)
data class DraftTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankEventId: Long,
    val ruleId: Long?,
    val suggestedTitle: String,
    val suggestedType: String,
    val suggestedAmountRials: Long,
    val suggestedCategoryId: Long?,
    val suggestedAccountId: Long?,
    val suggestedNote: String?,
    val status: String,
    val createdAt: Long,
    val resolvedAt: Long?,
)
