package ir.hesabino.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import ir.hesabino.app.data.local.db.dao.AccountDao
import ir.hesabino.app.data.local.db.dao.BankEventDao
import ir.hesabino.app.data.local.db.dao.CategoryDao
import ir.hesabino.app.data.local.db.dao.DraftDao
import ir.hesabino.app.data.local.db.dao.RuleDao
import ir.hesabino.app.data.local.db.dao.TransactionDao
import ir.hesabino.app.data.local.db.entity.AccountEntity
import ir.hesabino.app.data.local.db.entity.AutomationRuleEntity
import ir.hesabino.app.data.local.db.entity.BankEventEntity
import ir.hesabino.app.data.local.db.entity.CategoryEntity
import ir.hesabino.app.data.local.db.entity.DraftTransactionEntity
import ir.hesabino.app.data.local.db.entity.RuleActionEntity
import ir.hesabino.app.data.local.db.entity.RuleConditionEntity
import ir.hesabino.app.data.local.db.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BankEventEntity::class,
        AutomationRuleEntity::class,
        RuleConditionEntity::class,
        RuleActionEntity::class,
        DraftTransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HesabinoDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun bankEvents(): BankEventDao
    abstract fun rules(): RuleDao
    abstract fun drafts(): DraftDao

    companion object {
        const val NAME = "hesabino.db"

        /** الگوی مهاجرت از روز اول؛ v1→v2 اینجا اضافه می‌شود. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
