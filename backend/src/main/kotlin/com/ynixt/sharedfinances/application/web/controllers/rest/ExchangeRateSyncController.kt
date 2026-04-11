package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.config.security.OnlyServiceSecretAllowed
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/exchange-rates/sync")
@OnlyServiceSecretAllowed
@Tag(
    name = "Exchange Rate Sync",
    description = "Service-secret-only operations for exchange rate synchronization",
)
class ExchangeRateSyncController(
    private val exchangeRateService: ExchangeRateService,
    private val clock: Clock,
) {
    @Operation(summary = "Force sync exchange rates for a specific date")
    @PostMapping
    suspend fun syncForDate(
        @RequestParam(required = false) date: String?,
        @RequestParam(required = false) quotes: List<String>?,
    ): SyncResult {
        val targetDate =
            if (date.isNullOrBlank()) {
                LocalDate.now(clock)
            } else {
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            }

        val baseCurrencies = quotes?.map { it.uppercase() }?.toSet()?.takeIf { it.isNotEmpty() }

        val upserted = exchangeRateService.syncQuotesForDate(date = targetDate, baseCurrencies = baseCurrencies)

        return SyncResult(date = targetDate.toString(), upserted = upserted)
    }

    data class SyncResult(
        val date: String,
        val upserted: Int,
    )
}
