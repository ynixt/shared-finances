package com.ynixt.shared_finances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SharedFinancesApplication

fun main(args: Array<String>) {
	runApplication<SharedFinancesApplication>(*args)
}
