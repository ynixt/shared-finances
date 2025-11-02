package com.ynixt.sharedfinances

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Keep tests in this module runnable without external services by default.
 * This is a minimal example showing how to add a plain unit test.
 */
class SharedFinancesApplicationTests {
    @Test
    fun demoPureUnitTest() {
        val sum = listOf(1, 2, 3).sum()
        assertEquals(6, sum, "Sum should be 6")
    }
}
