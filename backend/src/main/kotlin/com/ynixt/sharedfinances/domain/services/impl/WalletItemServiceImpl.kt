package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class WalletItemServiceImpl(
    private val walletItemRepository: WalletItemRepository,
) : WalletItemService {
    override fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItemSearchResponse>> =
        createPage(pageable, countFn = { walletItemRepository.countByUserId(userId, enabled = true) }) {
            walletItemRepository.findAllByUserIdAndEnabled(
                userId = userId,
                enabled = true,
                pageable = pageable,
            )
        }
}
