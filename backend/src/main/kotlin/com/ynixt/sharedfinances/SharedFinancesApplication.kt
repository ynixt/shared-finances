package com.ynixt.sharedfinances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [DataRedisRepositoriesAutoConfiguration::class],
)
class SharedFinancesApplication

fun main(args: Array<String>) {
    runApplication<SharedFinancesApplication>(*args)
}
