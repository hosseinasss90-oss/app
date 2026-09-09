package ir.hesabino.app.data.backup

import ir.hesabino.app.data.repository.FinanceRepository
import ir.hesabino.app.domain.model.DisplayCurrency
import ir.hesabino.app.util.JalaliDate
import ir.hesabino.app.util.MoneyFormatter
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val finance: FinanceRepository,
) {
    suspend fun exportJson(): String {
        val txs = finance.allTransactions()
        val arr = JSONArray()
        txs.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("type", t.type.name)
                    .put("amountRials", t.amountRials)
                    .put("occurredAt", t.occurredAt)
                    .put("accountId", t.accountId)
                    .put("categoryId", t.categoryId)
                    .put("title", t.title)
                    .put("note", t.note ?: "")
                    .put("tags", t.tags.joinToString("|"))
                    .put("createdFrom", t.createdFrom.name),
            )
        }
        return JSONObject()
            .put("app", "hesabino")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("transactions", arr)
            .toString(2)
    }

    suspend fun exportCsv(currency: DisplayCurrency, persianDigits: Boolean): String {
        val txs = finance.allTransactions()
        val header = "date,type,title,amount,amount_rials,account_id,category_id,note,source"
        val rows = txs.map { t ->
            val date = JalaliDate.from(t.occurredAt).format(persianDigits = false)
            val amount = MoneyFormatter.format(t.amountRials, currency, persianDigits = false, withUnit = false)
            listOf(
                date,
                t.type.name,
                csv(t.title),
                amount,
                t.amountRials.toString(),
                t.accountId.toString(),
                t.categoryId?.toString().orEmpty(),
                csv(t.note.orEmpty()),
                t.createdFrom.name,
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csv(s: String): String {
        val n = s.replace("\"", "\"\"")
        return "\"$n\""
    }
}
