package com.ynixt.sharedfinances.support.config

import com.ynixt.sharedfinances.support.util.MutableTestClock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock

@TestConfiguration
class TestClockConfig {
    @Bean
    fun mutableTestClock(): MutableTestClock = MutableTestClock()

    @Bean
    @Primary
    fun testClock(mutableTestClock: MutableTestClock): Clock = mutableTestClock
}
