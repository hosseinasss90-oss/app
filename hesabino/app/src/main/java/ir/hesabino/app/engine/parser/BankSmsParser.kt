package ir.hesabino.app.engine.parser

import ir.hesabino.app.domain.model.AmountParserUnit
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.ParseStatus
import ir.hesabino.app.engine.normalize.AmountParser

data class ParseResult(
    val bankKey: String,
    val type: BankEventType,
    val amountRials: Long?,
    val unit: AmountParserUnit,
    val cardLast4: String?,
    val balanceRials: Long?,
    val description: String?,
    val status: ParseStatus,
)

interface BankSmsParser {
    val key: String
    val displayName: String
    fun canParse(senderId: String, body: String): Boolean
    fun parse(senderId: String, body: String, receivedAt: Long): ParseResult
}

fun AmountParser.Unit.toDomain(): AmountParserUnit = when (this) {
    AmountParser.Unit.RIAL -> AmountParserUnit.RIAL
    AmountParser.Unit.TOMAN -> AmountParserUnit.TOMAN
    AmountParser.Unit.UNKNOWN -> AmountParserUnit.UNKNOWN
}

object CardLast4Extractor {
    private val patterns = listOf(
        Regex("""\*+(\d{4})"""),
        Regex("""(?:کارت|حساب|card)[^\d]{0,8}(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""منتهی\s*به\s*(\d{4})"""),
        Regex("""آخرین\s*رقم[^\d]{0,6}(\d{4})"""),
        Regex("""ending\s*(\d{4})""", RegexOption.IGNORE_CASE),
    )

    fun extract(bodyLatin: String): String? {
        for (p in patterns) {
            p.find(bodyLatin)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }
}

object TypeDetector {
    private val debitKeys = listOf(
        "برداشت", "خرید", "پرداخت", "کسر", "برداشت از", "انتقال از",
        "برداشت‌از", "debit", "purchase", "withdraw", "خرید از",
    )
    private val creditKeys = listOf(
        "واریز", "دریافت", "واریز به", "وصول", "credit", "deposit", "received",
        "انتقال به حساب شما", "شارژ حساب",
    )

    fun detect(body: String): BankEventType {
        val t = body.lowercase()
        val debit = debitKeys.any { t.contains(it.lowercase()) }
        val credit = creditKeys.any { t.contains(it.lowercase()) }
        return when {
            debit && !credit -> BankEventType.DEBIT
            credit && !debit -> BankEventType.CREDIT
            debit && credit -> BankEventType.UNKNOWN
            else -> BankEventType.UNKNOWN
        }
    }
}
