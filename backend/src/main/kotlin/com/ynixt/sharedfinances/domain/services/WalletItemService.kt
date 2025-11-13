package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletItemService {
    fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItemSearchResponse>>
}
