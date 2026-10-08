package ai.hev.app.ui.components

import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `fixed decimals round half away from zero`() {
        assertEquals("33.3", formatFixed(33.333, decimals = 1))
        assertEquals("100.0", formatFixed(99.95, decimals = 1))
        assertEquals("0.50", formatFixed(0.495, decimals = 2))
        assertEquals("-1.25", formatFixed(-1.249, decimals = 2))
        assertEquals("0.00", formatFixed(-0.001, decimals = 2))
    }

    @Test
    fun `scores show decimals only when fractional`() {
        assertEquals("2", formatScore(2.0))
        assertEquals("1.78", formatScore(1.7838686319784252))
    }

    @Test
    fun `timestamps use month, day, hour and minute`() {
        assertEquals("10-08 15:04", formatTimestamp(1_791_443_040_000, TimeZone.of("Asia/Shanghai")))
    }
}
