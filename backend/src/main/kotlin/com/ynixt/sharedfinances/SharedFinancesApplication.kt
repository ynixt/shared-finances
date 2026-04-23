package com.ynixt.sharedfinances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        DataRedisRepositoriesAutoConfiguration::class,
        DataSourceTransactionManagerAutoConfiguration::class,
        MailSenderAutoConfiguration::class,
    ],
)
class SharedFinancesApplication

fun main(args: Array<String>) {
    runApplication<SharedFinancesApplication>(*args)
}
