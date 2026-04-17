package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuotePair
import reactor.core.publisher.Flux
import java.time.LocalDate

interface ExchangeRateQuoteBatchRepository {
    fun findClosestOnOrBeforeDateForPairs(
        pairs: Set<ExchangeRateQuotePair>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun findClosestOnOrAfterDateForPairs(
        pairs: Set<ExchangeRateQuotePair>,
        referenceDate: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>

    fun findAllByPairsAndQuoteDateBetween(
        pairs: Set<ExchangeRateQuotePair>,
        quoteDateFrom: LocalDate,
        quoteDateTo: LocalDate,
    ): Flux<ExchangeRateQuoteEntity>
}
