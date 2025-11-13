package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupWalletItemService {
    fun findAllItems(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItemSearchResponse>>
}
