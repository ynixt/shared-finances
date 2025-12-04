package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

@Service
class WalletItemServiceImpl(
    override val repository: WalletItemRepository,
    private val walletItemMapper: WalletItemMapper,
) : EntityServiceImpl<WalletItemEntity, WalletItem>(),
    WalletItemService {
    override fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItem>> =
        createPage(pageable, countFn = { repository.countByUserIdAndEnabled(userId, enabled = true) }) {
            repository
                .findAllByUserIdAndEnabled(
                    userId = userId,
                    enabled = true,
                    pageable = pageable,
                ).map(this::convert)
        }

    override fun findOne(id: UUID): Mono<WalletItem> = repository.findOneById(id).map(this::convert)

    override fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Mono<Long> = repository.addBalanceById(id, balance)

    override fun convert(entity: WalletItemEntity): WalletItem = walletItemMapper.toModel(entity)
}
