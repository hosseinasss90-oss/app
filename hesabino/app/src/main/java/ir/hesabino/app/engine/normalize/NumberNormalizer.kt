package ir.hesabino.app.engine.normalize

/**
 * تبدیل ارقام فارسی/عربی به لاتین و یکدست‌سازی جداکننده‌ها.
 * کاملاً بدون وابستگی Android — مناسب تست واحد.
 */
object NumberNormalizer {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun toLatinDigits(input: String): String {
        if (input.isEmpty()) return input
        val out = StringBuilder(input.length)
        for (ch in input) {
            val p = persianDigits.indexOf(ch)
            if (p >= 0) {
                out.append(('0'.code + p).toChar())
                continue
            }
            val a = arabicDigits.indexOf(ch)
            if (a >= 0) {
                out.append(('0'.code + a).toChar())
                continue
            }
            out.append(ch)
        }
        return out.toString()
    }

    /**
     * فاصله‌ها و جداکننده‌های هزارگان رایج در SMS ایرانی را حذف/یکدست می‌کند
     * ولی نقطه اعشار را نگه می‌دارد (در عمل بانک‌ها مبلغ صحیح می‌فرستند).
     */
    fun stripGroupingSeparators(input: String): String {
        return input
            .replace("\u00A0", "")
            .replace("\u200C", "") // ZWNJ
            .replace("٬", "")
            .replace("،", "")
            .replace(",", "")
            .replace(" ", "")
            .replace("'", "")
            .replace("’", "")
    }

    fun normalizeText(input: String): String {
        return toLatinDigits(input)
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("ة", "ه")
            .replace('\u200C', ' ')
            .replace(Regex("[\\t\\n\\r]+"), " ")
            .replace(Regex(" +"), " ")
            .trim()
    }

    /** بدنهٔ canonical برای fingerprint: بدون خط مانده، اعداد لاتین، فاصله یکدست. */
    fun canonicalizeBodyForFingerprint(body: String): String {
        val normalized = normalizeText(body)
        val withoutBalance = normalized
            .replace(Regex("""مانده[:\s]*[0-9۰-۹٠-٩,،٬.\s]+"""), "")
            .replace(Regex("""موجودی[:\s]*[0-9۰-۹٠-٩,،٬.\s]+"""), "")
            .replace(Regex("""balance[:\s]*[0-9,.\s]+""", RegexOption.IGNORE_CASE), "")
        return withoutBalance.replace(Regex(" +"), " ").trim()
    }
}
