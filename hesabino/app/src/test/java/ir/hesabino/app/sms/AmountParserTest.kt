package ir.hesabino.app.sms

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.engine.normalize.AmountParser
import org.junit.Test

class AmountParserTest {

    @Test
    fun parseLabeledRial() {
        val p = AmountParser.parse("بانک ملت\nبرداشت از کارت\nمبلغ: 1,200,000 ریال")
        assertThat(p).isNotNull()
        assertThat(p!!.rials).isEqualTo(1_200_000L)
        assertThat(p.unit).isEqualTo(AmountParser.Unit.RIAL)
    }

    @Test
    fun parsePersianDigitsToman() {
        val p = AmountParser.parse("خرید ۱۲۰۰۰۰ تومان")
        assertThat(p).isNotNull()
        assertThat(p!!.rials).isEqualTo(1_200_000L)
        assertThat(p.unit).isEqualTo(AmountParser.Unit.TOMAN)
    }

    @Test
    fun parseNumberThenUnit() {
        val p = AmountParser.parse("واریز 8500000 ريال به حساب")
        assertThat(p).isNotNull()
        assertThat(p!!.rials).isEqualTo(8_500_000L)
    }

    @Test
    fun fallbackLargestNumber() {
        val p = AmountParser.parse("کد پیگیری 12 مبلغ 45000 بدون واحد")
        assertThat(p).isNotNull()
        assertThat(p!!.rawNumber).isEqualTo(45_000L)
    }

    @Test
    fun emptyReturnsNull() {
        assertThat(AmountParser.parse("سلام خوبی؟")).isNull()
    }

    @Test
    fun parseNumberCleansSeparators() {
        assertThat(AmountParser.parseNumber("1٬200٬000")).isEqualTo(1_200_000L)
        assertThat(AmountParser.parseNumber("۱۲,۰۰۰")).isEqualTo(12_000L)
    }
}
