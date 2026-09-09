package ir.hesabino.app.sms

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.domain.model.BankEventType
import ir.hesabino.app.domain.model.ParseStatus
import ir.hesabino.app.engine.parser.GenericBankParser
import ir.hesabino.app.engine.parser.ParserRegistry
import org.junit.Test

class GenericBankParserTest {

    private val generic = GenericBankParser()
    private val registry = ParserRegistry()

    @Test
    fun parseMellatDebit() {
        val body = """
            بانک ملت
            برداشت از کارت *1234
            مبلغ: 1,200,000 ریال
            1405/06/18 14:32
            مانده: 45,000,000 ریال
        """.trimIndent()
        val r = generic.parse("BMJI", body, 0L)
        assertThat(r.type).isEqualTo(BankEventType.DEBIT)
        assertThat(r.amountRials).isEqualTo(1_200_000L)
        assertThat(r.cardLast4).isEqualTo("1234")
        assertThat(r.balanceRials).isEqualTo(45_000_000L)
        assertThat(r.status).isEqualTo(ParseStatus.PARSED)
    }

    @Test
    fun parseCredit() {
        val body = "واریز 8,200,000 ریال به حساب شما"
        val r = generic.parse("1700", body, 0L)
        assertThat(r.type).isEqualTo(BankEventType.CREDIT)
        assertThat(r.amountRials).isEqualTo(8_200_000L)
    }

    @Test
    fun registryPicksMellat() {
        val r = registry.parse("BMJI", "بانک ملت برداشت 50000 ریال", 0L)
        assertThat(r).isNotNull()
        assertThat(r!!.bankKey).isEqualTo("mellat")
    }

    @Test
    fun nonBankRejected() {
        assertThat(generic.canParse("0912", "سلام خوبی؟ فردا میای")).isFalse()
    }

    @Test
    fun persianAmountAndLast4() {
        val body = "خرید از فروشگاه مبلغ ۱۲۰۰۰۰ تومان کارت منتهی به ۴۵۶۷"
        val r = generic.parse("50001", body, 0L)
        assertThat(r.amountRials).isEqualTo(1_200_000L)
        assertThat(r.cardLast4).isEqualTo("4567")
    }
}
