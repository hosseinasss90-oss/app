package ir.hesabino.app.domain

import com.google.common.truth.Truth.assertThat
import ir.hesabino.app.util.JalaliDate
import org.junit.Test
import java.time.LocalDate

class JalaliDateTest {

    @Test
    fun knownDate2026Sep09() {
        val j = JalaliDate.from(LocalDate.of(2026, 9, 9))
        assertThat(j.year).isEqualTo(1405)
        assertThat(j.month).isEqualTo(6)
        assertThat(j.day).isEqualTo(18)
        assertThat(j.formatLong(persianDigits = false)).isEqualTo("18 شهریور 1405")
    }

    @Test
    fun nowruz1405() {
        val j = JalaliDate.from(LocalDate.of(2026, 3, 21))
        assertThat(j).isEqualTo(JalaliDate(1405, 1, 1))
        assertThat(j.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 21))
    }

    @Test
    fun roundTrip() {
        val d = LocalDate.of(2024, 11, 2)
        assertThat(JalaliDate.from(d).toLocalDate()).isEqualTo(d)
    }
}
