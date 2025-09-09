package com.ynixt.sharedfinances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SharedFinancesApplication

fun main(args: Array<String>) {
    runApplication<SharedFinancesApplication>(*args)
}
