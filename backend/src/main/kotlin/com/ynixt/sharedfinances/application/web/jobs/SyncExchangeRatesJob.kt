package com.ynixt.sharedfinances.application.web.jobs

import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class SyncExchangeRatesJob(
    private val exchangeRateService: ExchangeRateService,
) {
    private val logger = LoggerFactory.getLogger(SyncExchangeRatesJob::class.java)

    @Scheduled(cron = "0 0 0/12 * * 1")
    fun job() {
        logger.info("Sync exchange rates job started")

        runOnce()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess { synced -> logger.info("Sync exchange rates job finished successfully. Upserted $synced rates.") }
            .doOnError { ex -> logger.error("Sync exchange rates job failed", ex) }
            .subscribe()
    }

    fun runOnce(): Mono<Int> =
        mono {
            exchangeRateService.syncLatestQuotes()
        }
}
