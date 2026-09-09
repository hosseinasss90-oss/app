package ir.hesabino.app.engine.normalize

/**
 * استخراج مبلغ و واحد از متن SMS بانکی ایران.
 * خروجی همیشه ریال است (Long).
 */
object AmountParser {

    enum class Unit { RIAL, TOMAN, UNKNOWN }

    data class ParsedAmount(
        val rials: Long,
        val unit: Unit,
        val rawNumber: Long,
        val matchedText: String,
    )

    /**
     * الگوهای مبلغ، از صریح به ضمنی:
     * 1) مبلغ: 1,200,000 ریال
     * 2) 1,200,000 ریال
     * 3) مبلغ 120000
     * 4) بزرگ‌ترین عدد >= 1000 در متن (fallback ضعیف)
     */
    private val labeled = Regex(
        """(?:مبلغ|amount|AMT|sum)[:\s]*([0-9][0-9,،٬\s]{2,})(?:\s*(ریال|ريال|﷼|IRR|rial|toman|تومان|تومن))?""",
        RegexOption.IGNORE_CASE,
    )
    private val numberThenUnit = Regex(
        """([0-9][0-9,،٬\s]{2,})\s*(ریال|ريال|﷼|IRR|rial|toman|تومان|تومن)""",
        RegexOption.IGNORE_CASE,
    )
    private val anyBigNumber = Regex("""([0-9]{1,3}(?:[,،٬][0-9]{3})+|[0-9]{4,})""")

    fun parse(rawBody: String, defaultUnit: Unit = Unit.RIAL): ParsedAmount? {
        val body = NumberNormalizer.toLatinDigits(rawBody)
        labeled.find(body)?.let { m -> fromMatch(body, m, defaultUnit)?.let { return it } }
        numberThenUnit.find(body)?.let { m -> fromMatch(body, m, defaultUnit)?.let { return it } }

        val candidates = anyBigNumber.findAll(body)
            .mapNotNull { m ->
                val n = parseNumber(m.groupValues[1]) ?: return@mapNotNull null
                n to m
            }
            .filter { it.first >= 1_000L }
            .toList()
        if (candidates.isEmpty()) return null
        val (n, m) = candidates.maxBy { it.first }
        val unit = detectUnitAround(body, m.range) ?: defaultUnit
        return ParsedAmount(
            rials = toRials(n, unit),
            unit = unit,
            rawNumber = n,
            matchedText = m.value,
        )
    }

    fun parseNumber(raw: String): Long? {
        val cleaned = NumberNormalizer.stripGroupingSeparators(
            NumberNormalizer.toLatinDigits(raw),
        ).substringBefore('.')
        if (cleaned.isEmpty() || !cleaned.all { it.isDigit() }) return null
        return cleaned.toLongOrNull()
    }

    fun toRials(number: Long, unit: Unit): Long = when (unit) {
        Unit.TOMAN -> number * 10L
        Unit.RIAL, Unit.UNKNOWN -> number
    }

    fun detectUnit(text: String): Unit {
        val t = NumberNormalizer.normalizeText(text).lowercase()
        return when {
            t.contains("تومان") || t.contains("تومن") || t.contains("toman") -> Unit.TOMAN
            t.contains("ریال") || t.contains("rial") || t.contains("irr") || t.contains("﷼") -> Unit.RIAL
            else -> Unit.UNKNOWN
        }
    }

    private fun fromMatch(body: String, match: MatchResult, defaultUnit: Unit): ParsedAmount? {
        val numberRaw = match.groupValues[1]
        val n = parseNumber(numberRaw) ?: return null
        val unitText = match.groupValues.getOrNull(2).orEmpty()
        val unit = if (unitText.isBlank()) {
            detectUnitAround(body, match.range)
                ?: detectUnit(match.value).let { if (it == Unit.UNKNOWN) defaultUnit else it }
        } else {
            detectUnit(unitText).let { if (it == Unit.UNKNOWN) defaultUnit else it }
        }
        return ParsedAmount(
            rials = toRials(n, unit),
            unit = unit,
            rawNumber = n,
            matchedText = match.value.trim(),
        )
    }

    private fun detectUnitAround(body: String, range: IntRange): Unit? {
        val start = (range.first - 12).coerceAtLeast(0)
        val end = (range.last + 12).coerceAtMost(body.lastIndex)
        val window = body.substring(start, end + 1)
        val u = detectUnit(window)
        return u.takeIf { it != Unit.UNKNOWN }
    }
}
