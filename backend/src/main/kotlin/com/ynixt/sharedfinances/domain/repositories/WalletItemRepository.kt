package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletItemRepository {
    fun findAllByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemSearchResponse>

    fun countByUserId(
        userId: UUID,
        enabled: Boolean,
    ): Mono<Long>
}
