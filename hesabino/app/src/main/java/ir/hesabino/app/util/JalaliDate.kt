package ir.hesabino.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * تبدیل میلادی ↔ شمسی با الگوریتم جلالی استاندارد.
 * Zone پیش‌فرض: Asia/Tehran
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    fun toLocalDate(): LocalDate = jalaliToGregorian(year, month, day)

    fun format(persianDigits: Boolean = true): String {
        val raw = "%04d/%02d/%02d".format(year, month, day)
        return if (persianDigits) raw.toPersianDigits() else raw
    }

    fun formatLong(persianDigits: Boolean = true): String {
        val raw = "$day ${monthName(month)} $year"
        return if (persianDigits) raw.toPersianDigits() else raw
    }

    companion object {
        val MONTHS = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
        val WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

        fun monthName(month: Int): String = MONTHS.getOrElse(month - 1) { "" }

        fun from(epochMillis: Long, zone: ZoneId = ZoneId.of("Asia/Tehran")): JalaliDate {
            val d = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            return from(d)
        }

        fun from(date: LocalDate): JalaliDate {
            val (y, m, d) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
            return JalaliDate(y, m, d)
        }

        fun now(zone: ZoneId = ZoneId.of("Asia/Tehran")): JalaliDate =
            from(ZonedDateTime.now(zone).toLocalDate())

        fun monthRange(year: Int, month: Int, zone: ZoneId = ZoneId.of("Asia/Tehran")): Pair<Long, Long> {
            val start = jalaliToGregorian(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val next = if (month == 12) jalaliToGregorian(year + 1, 1, 1) else jalaliToGregorian(year, month + 1, 1)
            val end = next.atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return start to end
        }
    }
}

fun String.toPersianDigits(): String {
    val map = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val out = StringBuilder(length)
    for (ch in this) {
        out.append(if (ch in '0'..'9') map[ch - '0'] else ch)
    }
    return out.toString()
}

fun String.toLatinDigits(): String {
    val p = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val a = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val out = StringBuilder(length)
    for (ch in this) {
        val i = p.indexOf(ch)
        if (i >= 0) {
            out.append('0' + i); continue
        }
        val j = a.indexOf(ch)
        if (j >= 0) {
            out.append('0' + j); continue
        }
        out.append(ch)
    }
    return out.toString()
}

internal fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
    val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var gy2 = gy - 1600
    val gm2 = gm - 1
    val gd2 = gd - 1
    var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
    gDayNo += gDaysInMonth[gm2] + gd2
    if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) gDayNo += 1
    var jDayNo = gDayNo - 79
    val jNp = jDayNo / 12053
    jDayNo %= 12053
    var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (jDayNo < 186) {
        jm = 1 + jDayNo / 31
        jd = 1 + jDayNo % 31
    } else {
        jm = 7 + (jDayNo - 186) / 30
        jd = 1 + (jDayNo - 186) % 30
    }
    return intArrayOf(jy, jm, jd)
}

internal fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
    var jy2 = jy - 979
    val jm2 = jm - 1
    val jd2 = jd - 1
    var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4
    for (i in 0 until jm2) {
        jDayNo += if (i < 6) 31 else 30
    }
    jDayNo += jd2
    var gDayNo = jDayNo + 79
    var gy = 1600 + 400 * (gDayNo / 146097)
    gDayNo %= 146097
    var leap = true
    if (gDayNo >= 36525) {
        gDayNo--
        gy += 100 * (gDayNo / 36524)
        gDayNo %= 36524
        if (gDayNo >= 365) gDayNo++ else leap = false
    }
    gy += 4 * (gDayNo / 1461)
    gDayNo %= 1461
    if (gDayNo >= 366) {
        leap = false
        gDayNo--
        gy += gDayNo / 365
        gDayNo %= 365
    }
    val gDays = intArrayOf(
        31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
    )
    var gm = 0
    while (gm < 12 && gDayNo >= gDays[gm]) {
        gDayNo -= gDays[gm]
        gm++
    }
    return LocalDate.of(gy, gm + 1, gDayNo + 1)
}
