package com.ynixt.sharedfinances.domain.services.walletentry

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface WalletEntryTransferQuoteService {
    suspend fun quote(
        userId: UUID,
        request: TransferQuoteRequest,
    ): TransferQuoteResult

    suspend fun transferRate(
        userId: UUID,
        request: TransferRateRequest,
    ): TransferRateResult
}

data class TransferQuoteRequest(
    val groupId: UUID? = null,
    val originId: UUID,
    val targetId: UUID,
    val date: LocalDate,
    val originValue: BigDecimal,
)

data class TransferQuoteResult(
    val targetValue: BigDecimal,
)

data class TransferRateRequest(
    val groupId: UUID? = null,
    val originId: UUID,
    val targetId: UUID,
    val date: LocalDate,
)

data class TransferRateResult(
    val rate: BigDecimal,
    val quoteDate: LocalDate,
    val baseCurrency: String,
    val quoteCurrency: String,
)
