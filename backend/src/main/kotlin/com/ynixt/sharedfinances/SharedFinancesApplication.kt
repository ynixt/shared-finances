package com.ynixt.sharedfinances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    exclude = [DataRedisRepositoriesAutoConfiguration::class],
)
@EnableScheduling
class SharedFinancesApplication

fun main(args: Array<String>) {
    runApplication<SharedFinancesApplication>(*args)
}
