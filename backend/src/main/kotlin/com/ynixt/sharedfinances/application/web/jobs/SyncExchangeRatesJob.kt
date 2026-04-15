package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SyncExchangeRatesJob(
    private val exchangeRateService: ExchangeRateService,
) {
    private val logger = LoggerFactory.getLogger(SyncExchangeRatesJob::class.java)

    suspend fun execute() {
        logger.info("Sync exchange rates job started")
        val synced = exchangeRateService.syncLatestQuotes()
        logger.info("Sync exchange rates job finished successfully. Upserted $synced rates.")
    }

    fun runOnce(): Mono<Int> =
        mono {
            exchangeRateService.syncLatestQuotes()
        }
}
