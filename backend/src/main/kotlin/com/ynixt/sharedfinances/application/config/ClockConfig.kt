package com.ynixt.sharedfinances.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {
    @Bean
    fun appClock(): Clock = Clock.systemDefaultZone()
}
