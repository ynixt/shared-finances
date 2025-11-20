package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.WalletItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

interface WalletItemService {
    fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItem>>

    fun findOne(id: UUID): Mono<WalletItem>

    fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Mono<Long>
}
