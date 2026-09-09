package ir.hesabino.app.sms

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.engine.fingerprint.Fingerprint
import org.junit.Test

class FingerprintTest {

    private val body = "بانک ملت\nبرداشت از کارت *1234\nمبلغ: 1,200,000 ریال\nمانده: 45,000,000 ریال"

    @Test
    fun sameSmsSameMinuteIsDuplicate() {
        val t = 1_725_000_000_000L
        val a = Fingerprint.compute("BMJI", 1_200_000, "1234", t, body)
        val b = Fingerprint.compute("BMJI", 1_200_000, "1234", t + 10_000, body)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun balanceChangeDoesNotChangeFingerprint() {
        val t = 1_725_000_000_000L
        val body2 = body.replace("45,000,000", "43,000,000")
        val a = Fingerprint.compute("BMJI", 1_200_000, "1234", t, body)
        val b = Fingerprint.compute("BMJI", 1_200_000, "1234", t, body2)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun differentAmountIsDifferentEvent() {
        val t = 1_725_000_000_000L
        val a = Fingerprint.compute("BMJI", 1_200_000, "1234", t, body)
        val b = Fingerprint.compute("BMJI", 2_000_000, "1234", t, body.replace("1,200,000", "2,000,000"))
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun reimportAfterRestartSameFingerprint() {
        val t = 1_725_000_123_456L
        val a = Fingerprint.compute("1700", 1_200_000, "1234", t, body)
        val b = Fingerprint.compute("1700", 1_200_000, "1234", t, body)
        assertThat(a).isEqualTo(b)
        assertThat(a).hasLength(64)
    }

    @Test
    fun nextMinuteIsNewEvent() {
        val t = 1_725_000_000_000L
        val a = Fingerprint.compute("BMJI", 1_200_000, "1234", t, body)
        val b = Fingerprint.compute("BMJI", 1_200_000, "1234", t + Fingerprint.TIME_BUCKET_MS, body)
        assertThat(a).isNotEqualTo(b)
    }
}
