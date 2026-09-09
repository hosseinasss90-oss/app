package ir.hesabino.app.util

import ir.hesabino.app.domain.model.DisplayCurrency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private const val RIALS_PER_TOMAN = 10L

    fun format(
        rials: Long,
        currency: DisplayCurrency,
        persianDigits: Boolean,
        withUnit: Boolean = true,
        signed: Boolean = false,
    ): String {
        val value = when (currency) {
            DisplayCurrency.TOMAN -> rials / RIALS_PER_TOMAN
            DisplayCurrency.RIAL -> rials
        }
        val abs = kotlin.math.abs(value)
        val grouped = group(abs)
        val digits = if (persianDigits) grouped.toPersianDigits() else grouped
        val sign = when {
            !signed -> ""
            value < 0 -> "−"
            value > 0 -> "+"
            else -> ""
        }
        val unit = if (!withUnit) "" else when (currency) {
            DisplayCurrency.TOMAN -> " ت"
            DisplayCurrency.RIAL -> " ر"
        }
        return sign + digits + unit
    }

    fun privacyMask(): String = "••••"

    fun parseUserInputToRials(raw: String, currency: DisplayCurrency): Long? {
        val latin = raw.toLatinDigits()
            .replace("٬", "")
            .replace("،", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        if (latin.isEmpty() || !latin.all { it.isDigit() }) return null
        val n = latin.toLongOrNull() ?: return null
        return when (currency) {
            DisplayCurrency.TOMAN -> n * RIALS_PER_TOMAN
            DisplayCurrency.RIAL -> n
        }
    }

    fun toDisplayNumber(rials: Long, currency: DisplayCurrency): Long = when (currency) {
        DisplayCurrency.TOMAN -> rials / RIALS_PER_TOMAN
        DisplayCurrency.RIAL -> rials
    }

    private fun group(n: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '٬'
        }
        return DecimalFormat("#,###", symbols).format(n)
    }
}
