package ir.hesabino.app.engine.parser

import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.ParseStatus
import ir.hesabino.app.engine.normalize.AmountParser
import ir.hesabino.app.engine.normalize.NumberNormalizer

/**
 * پارسر پیش‌فرض برای هر SMS که بوی بانکی می‌دهد.
 * بانک‌های اختصاصی باید قبل از این در Registry قرار بگیرند.
 */
class GenericBankParser : BankSmsParser {
    override val key: String = "generic"
    override val displayName: String = "عمومی"

    private val bankingHints = listOf(
        "بانک", "برداشت", "واریز", "مانده", "موجودی", "ریال", "ريال", "تومان",
        "کارت", "حساب", "شتاب", "خرید", "پرداخت", "bank", "irr", "rial",
    )

    override fun canParse(senderId: String, body: String): Boolean {
        val t = NumberNormalizer.normalizeText(body).lowercase()
        val looksBank = bankingHints.any { t.contains(it.lowercase()) }
        val hasAmount = AmountParser.parse(body) != null
        val senderLooksBank = senderId.any { it.isDigit() } && senderId.length in 4..14
        return (looksBank || senderLooksBank) && hasAmount
    }

    override fun parse(senderId: String, body: String, receivedAt: Long): ParseResult {
        val latin = NumberNormalizer.toLatinDigits(body)
        val amount = AmountParser.parse(latin)
        val type = TypeDetector.detect(latin)
        val last4 = CardLast4Extractor.extract(latin)
        val balance = parseBalance(latin)
        val status = when {
            amount != null && type != BankEventType.UNKNOWN -> ParseStatus.PARSED
            amount != null -> ParseStatus.PARTIAL
            else -> ParseStatus.UNPARSED
        }
        return ParseResult(
            bankKey = key,
            type = type,
            amountRials = amount?.rials,
            unit = amount?.unit.toDomainOrUnknown(),
            cardLast4 = last4,
            balanceRials = balance,
            description = NumberNormalizer.normalizeText(body).take(180),
            status = status,
        )
    }

    private fun parseBalance(latin: String): Long? {
        val m = Regex("""(?:مانده|موجودی)[:\s]*([0-9][0-9,،٬\s]+)""")
            .find(latin) ?: return null
        val n = AmountParser.parseNumber(m.groupValues[1]) ?: return null
        val unit = AmountParser.detectUnit(m.value)
        return AmountParser.toRials(n, if (unit == AmountParser.Unit.UNKNOWN) AmountParser.Unit.RIAL else unit)
    }
}

private fun AmountParser.Unit?.toDomainOrUnknown() = this?.toDomain()
    ?: ir.hesabino.app.domain.model.AmountParserUnit.UNKNOWN
