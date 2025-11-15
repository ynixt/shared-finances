package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
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
    private val walletItemMapper: WalletItemMapper,
) : WalletItemService {
    override fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItem>> =
        createPage(pageable, countFn = { walletItemRepository.countByUserIdAndEnabled(userId, enabled = true) }) {
            walletItemRepository
                .findAllByUserIdAndEnabled(
                    userId = userId,
                    enabled = true,
                    pageable = pageable,
                ).map(walletItemMapper::toModel)
        }
}
