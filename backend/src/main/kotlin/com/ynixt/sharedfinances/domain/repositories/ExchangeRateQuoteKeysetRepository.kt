package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import reactor.core.publisher.Flux
import java.time.LocalDate

interface ExchangeRateQuoteKeysetRepository {
    fun findQuotesKeyset(
        limit: Int,
        baseCurrency: String?,
        quoteCurrency: String?,
        quoteDateFrom: LocalDate?,
        quoteDateTo: LocalDate?,
        cursor: ExchangeRateQuoteListCursor?,
    ): Flux<ExchangeRateQuoteEntity>
}
