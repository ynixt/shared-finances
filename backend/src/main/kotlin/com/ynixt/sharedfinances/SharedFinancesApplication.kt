package com.ynixt.sharedfinances

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SharedFinancesApplication

fun main(args: Array<String>) {
	runApplication<SharedFinancesApplication>(*args)
}
