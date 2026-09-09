package ir.hesabino.app.engine.parser

import ir.hesabino.app.engine.normalize.NumberNormalizer

/**
 * پارسرهای اختصاصی بر اساس فرستنده و واژه‌های ثابت هر بانک.
 * الگوی مبلغ را به Generic می‌سپارند؛ فقط هویت بانک و canParse را دقیق می‌کنند.
 */
open class SenderBankParser(
    override val key: String,
    override val displayName: String,
    private val senderNeedles: List<String>,
    private val bodyNeedles: List<String> = emptyList(),
) : BankSmsParser {

    private val generic = GenericBankParser()

    override fun canParse(senderId: String, body: String): Boolean {
        val s = senderId.lowercase()
        if (senderNeedles.any { s.contains(it.lowercase()) }) return true
        val t = NumberNormalizer.normalizeText(body)
        return bodyNeedles.any { t.contains(it) }
    }

    override fun parse(senderId: String, body: String, receivedAt: Long) =
        generic.parse(senderId, body, receivedAt).copy(bankKey = key)
}

object IranianBankParsers {
    fun all(): List<BankSmsParser> = listOf(
        SenderBankParser(
            key = "mellat",
            displayName = "ملت",
            senderNeedles = listOf("bmji", "mellat", "1700", "983000"),
            bodyNeedles = listOf("بانک ملت"),
        ),
        SenderBankParser(
            key = "melli",
            displayName = "ملی",
            senderNeedles = listOf("bmi", "melli", "09622"),
            bodyNeedles = listOf("بانک ملی"),
        ),
        SenderBankParser(
            key = "saderat",
            displayName = "صادرات",
            senderNeedles = listOf("bsi", "saderat"),
            bodyNeedles = listOf("بانک صادرات"),
        ),
        SenderBankParser(
            key = "pasargad",
            displayName = "پاسارگاد",
            senderNeedles = listOf("bpi", "pasargad", "5022"),
            bodyNeedles = listOf("بانک پاسارگاد"),
        ),
        SenderBankParser(
            key = "tejarat",
            displayName = "تجارت",
            senderNeedles = listOf("tejarat", "btejarat"),
            bodyNeedles = listOf("بانک تجارت"),
        ),
        SenderBankParser(
            key = "saman",
            displayName = "سامان",
            senderNeedles = listOf("saman", "sb24"),
            bodyNeedles = listOf("بانک سامان"),
        ),
        SenderBankParser(
            key = "parsian",
            displayName = "پارسیان",
            senderNeedles = listOf("parsian"),
            bodyNeedles = listOf("بانک پارسیان"),
        ),
        SenderBankParser(
            key = "ayandeh",
            displayName = "آینده",
            senderNeedles = listOf("ba24", "ayandeh"),
            bodyNeedles = listOf("بانک آینده"),
        ),
        SenderBankParser(
            key = "resalat",
            displayName = "رسالت",
            senderNeedles = listOf("resalat", "rqbank"),
            bodyNeedles = listOf("قرض‌الحسنه رسالت", "بانک رسالت"),
        ),
        SenderBankParser(
            key = "keshavarzi",
            displayName = "کشاورزی",
            senderNeedles = listOf("bki", "keshavarzi"),
            bodyNeedles = listOf("بانک کشاورزی"),
        ),
    )
}

class ParserRegistry(
    extra: List<BankSmsParser> = emptyList(),
) {
    private val parsers: List<BankSmsParser> =
        extra + IranianBankParsers.all() + listOf(GenericBankParser())

    fun parse(senderId: String, body: String, receivedAt: Long): ParseResult? {
        val parser = parsers.firstOrNull { it.canParse(senderId, body) } ?: return null
        return parser.parse(senderId, body, receivedAt)
    }
}
