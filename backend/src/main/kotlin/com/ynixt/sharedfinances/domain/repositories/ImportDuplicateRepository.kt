package com.ynixt.sharedfinances.domain.repositories

import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface ImportDuplicateRepository {
    fun existsExact(
        userId: UUID,
        walletItemId: UUID,
        name: String?,
        value: BigDecimal,
        date: LocalDate,
        installment: Int?,
    ): Mono<Boolean>
}
