package com.ynixt.sharedfinances.domain.entities.exchangerate

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Table("exchange_rate_quote")
class ExchangeRateQuoteEntity(
    val source: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val quoteDate: LocalDate,
    val rate: BigDecimal,
    val quotedAt: OffsetDateTime,
    val fetchedAt: OffsetDateTime,
) : AuditedEntity()
