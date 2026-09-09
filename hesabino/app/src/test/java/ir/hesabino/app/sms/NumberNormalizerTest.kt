package ir.hesabino.app.sms

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.engine.normalize.NumberNormalizer
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun persianDigitsToLatin() {
        assertThat(NumberNormalizer.toLatinDigits("۱۲۳۴۵۶۷۸۹۰")).isEqualTo("1234567890")
    }

    @Test
    fun arabicIndicDigitsToLatin() {
        assertThat(NumberNormalizer.toLatinDigits("١٢٣٤")).isEqualTo("1234")
    }

    @Test
    fun mixedTextPreserved() {
        assertThat(NumberNormalizer.toLatinDigits("مبلغ ۱۲,۰۰۰ ریال")).isEqualTo("مبلغ 12,000 ریال")
    }

    @Test
    fun stripGrouping() {
        assertThat(NumberNormalizer.stripGroupingSeparators("1,200,000")).isEqualTo("1200000")
        assertThat(NumberNormalizer.stripGroupingSeparators("1٬200٬000")).isEqualTo("1200000")
        assertThat(NumberNormalizer.stripGroupingSeparators("1،200،000")).isEqualTo("1200000")
    }

    @Test
    fun canonicalizeDropsBalanceLine() {
        val a = NumberNormalizer.canonicalizeBodyForFingerprint(
            "برداشت از کارت *1234 مبلغ 1200000 ریال مانده: 45000000 ریال",
        )
        val b = NumberNormalizer.canonicalizeBodyForFingerprint(
            "برداشت از کارت *1234 مبلغ 1200000 ریال مانده: 43000000 ریال",
        )
        assertThat(a).isEqualTo(b)
        assertThat(a.contains("مانده")).isFalse()
    }
}
