package com.snapdex.app.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScanCounterServiceTest {

    @Test
    fun `same month — no reset needed`() {
        val now = Calendar.getInstance()
        assertTrue(isSameMonth(now.timeInMillis, now.timeInMillis))
    }

    @Test
    fun `different month — reset triggered`() {
        val now = Calendar.getInstance()
        val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        assertFalse(isSameMonth(lastMonth.timeInMillis, now.timeInMillis))
    }

    @Test
    fun `different year — reset triggered`() {
        val now = Calendar.getInstance()
        val lastYear = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        assertFalse(isSameMonth(lastYear.timeInMillis, now.timeInMillis))
    }

    @Test
    fun `zero stored timestamp — treated as distant past, reset triggered`() {
        assertFalse(isSameMonth(0L, Calendar.getInstance().timeInMillis))
    }

    @Test
    fun `FREE_MONTHLY_LIMIT constant is 10`() {
        assertEquals(10, ScanCounterService.FREE_MONTHLY_LIMIT)
    }

    private fun isSameMonth(storedMs: Long, nowMs: Long): Boolean {
        val stored = Calendar.getInstance().apply { timeInMillis = storedMs }
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        return stored.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            stored.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}
