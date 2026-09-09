package ir.hesabino.app.data.repository

import ir.hesabino.app.data.local.db.HesabinoDatabase
import ir.hesabino.app.data.local.db.entity.AccountEntity
import ir.hesabino.app.data.local.db.entity.CategoryEntity
import ir.hesabino.app.data.local.db.toDomain
import ir.hesabino.app.data.local.db.toEntity
import ir.hesabino.app.domain.model.Account
import ir.hesabino.app.domain.model.Category
import ir.hesabino.app.domain.model.MonthSummary
import ir.hesabino.app.domain.model.Transaction
import ir.hesabino.app.domain.model.TxType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val db: HesabinoDatabase,
) {
    private val accounts = db.accounts()
    private val categories = db.categories()
    private val transactions = db.transactions()

    fun observeAccounts(): Flow<List<Account>> = accounts.observeActive().map { it.map { e -> e.toDomain() } }
    fun observeCategories(): Flow<List<Category>> = categories.observeActive().map { it.map { e -> e.toDomain() } }

    fun observeTransactions(
        from: Long,
        to: Long,
        type: String? = null,
        accountId: Long? = null,
        categoryId: Long? = null,
        query: String = "",
    ): Flow<List<Transaction>> = transactions.observeFiltered(from, to, type, accountId, categoryId, query)
        .map { it.map { e -> e.toDomain() } }

    /** فقط بازهٔ زمانی (بدون فیلتر نوع/حساب/دسته) برای نمودارها. */
    fun observeRange(from: Long, to: Long): Flow<List<Transaction>> =
        transactions.observeRange(from, to).map { it.map { e -> e.toDomain() } }

    /** همهٔ تراکنش‌های فعال به ترتیب زمان برای محاسبهٔ بازهٔ واقعی داده‌ها و نمودار روزانه. */
    fun observeAllActive(): Flow<List<Transaction>> =
        transactions.observeAllActiveAsc().map { it.map { e -> e.toDomain() } }

    fun observeMonthSummary(from: Long, to: Long): Flow<MonthSummary> =
        combine(
            transactions.observeRange(from, to),
            categories.observeActive(),
        ) { txs, cats ->
            val catMap = cats.associateBy { it.id }
            val expense = txs.filter { it.type == TxType.EXPENSE.name }.sumOf { it.amountRials }
            val income = txs.filter { it.type == TxType.INCOME.name }.sumOf { it.amountRials }
            val byCat = txs.filter { it.type == TxType.EXPENSE.name }
                .groupBy { it.categoryId }
                .map { (id, list) ->
                    val sum = list.sumOf { it.amountRials }
                    val c = id?.let { catMap[it] }
                    MonthSummary.CategorySpend(
                        categoryId = id,
                        name = c?.name ?: "سایر",
                        iconKey = c?.iconKey ?: "other",
                        amountRials = sum,
                        share = if (expense == 0L) 0f else sum.toFloat() / expense.toFloat(),
                    )
                }
                .sortedByDescending { it.amountRials }
            MonthSummary(expense, income, income - expense, byCat)
        }

    suspend fun getTransaction(id: Long) = transactions.get(id)?.toDomain()

    suspend fun upsertTransaction(tx: Transaction): Long {
        val now = System.currentTimeMillis()
        return if (tx.id == 0L) {
            transactions.insert(tx.toEntity(now))
        } else {
            val existing = transactions.get(tx.id)
            transactions.update(
                tx.toEntity(now).copy(
                    createdAt = existing?.createdAt ?: now,
                    isDeleted = existing?.isDeleted ?: false,
                ),
            )
            tx.id
        }
    }

    suspend fun deleteTransaction(id: Long) = transactions.softDelete(id, System.currentTimeMillis())
    suspend fun restoreTransaction(id: Long) = transactions.restore(id, System.currentTimeMillis())
    suspend fun bySourceEvent(eventId: Long) = transactions.bySourceEvent(eventId)?.toDomain()

    suspend fun addAccount(account: Account): Long =
        accounts.insert(account.toEntity(System.currentTimeMillis()))

    suspend fun updateAccount(account: Account) {
        val existing = accounts.get(account.id) ?: return
        accounts.update(account.toEntity(existing.createdAt))
    }

    suspend fun archiveAccount(id: Long) = accounts.archive(id)

    suspend fun addCategory(category: Category): Long = categories.insert(category.toEntity())
    suspend fun updateCategory(category: Category) = categories.update(category.toEntity())
    suspend fun archiveCategory(id: Long) = categories.archive(id)
    suspend fun mergeCategories(from: Long, into: Long) = categories.merge(from, into)

    suspend fun allTransactions() = transactions.allActive().map { it.toDomain() }

    suspend fun seedIfEmpty() {
        if (categories.count() > 0) return
        val now = System.currentTimeMillis()
        accounts.insert(AccountEntity(0, "نقدی", "CASH", null, 0xFF0F6E56.toInt(), 0, false, now))
        accounts.insert(AccountEntity(0, "کارت ملت", "CARD", "1234", 0xFF2F6F8F.toInt(), 1, false, now))
        val seed = listOf(
            CategoryEntity(0, "خوراک", "grocery", "EXPENSE", null, 0, false),
            CategoryEntity(0, "حمل‌ونقل", "transport", "EXPENSE", null, 1, false),
            CategoryEntity(0, "مسکن", "home", "EXPENSE", null, 2, false),
            CategoryEntity(0, "قبوض", "bills", "EXPENSE", null, 3, false),
            CategoryEntity(0, "سلامت", "health", "EXPENSE", null, 4, false),
            CategoryEntity(0, "پوشاک", "clothes", "EXPENSE", null, 5, false),
            CategoryEntity(0, "سرگرمی", "fun", "EXPENSE", null, 6, false),
            CategoryEntity(0, "سایر", "other", "EXPENSE", null, 7, false),
            CategoryEntity(0, "حقوق", "salary", "INCOME", null, 8, false),
            CategoryEntity(0, "هدیه", "gift", "INCOME", null, 9, false),
            CategoryEntity(0, "سایر درآمد", "other_in", "INCOME", null, 10, false),
            CategoryEntity(0, "انتقال", "transfer", "TRANSFER", null, 11, false),
        )
        categories.insertAll(seed)
    }
}
