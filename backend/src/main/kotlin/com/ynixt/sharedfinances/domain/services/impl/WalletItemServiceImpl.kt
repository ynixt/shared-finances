package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class WalletItemServiceImpl(
    override val repository: WalletItemRepository,
    private val walletItemMapper: WalletItemMapper,
) : EntityServiceImpl<WalletItemEntity, WalletItem>(),
    WalletItemService {
    override suspend fun findAllItems(
        userId: UUID,
        pageable: Pageable,
    ): Page<WalletItem> =
        createPage(pageable, countFn = { repository.countByUserIdAndEnabled(userId, enabled = true) }) {
            repository
                .findAllByUserIdAndEnabled(
                    userId = userId,
                    enabled = true,
                    pageable = pageable,
                ).map(this::convert)
        }

    override suspend fun findOne(id: UUID): WalletItem? = repository.findOneById(id).map(this::convert).awaitSingleOrNull()

    override suspend fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Long = repository.addBalanceById(id, balance).awaitSingle()

    override fun convert(entity: WalletItemEntity): WalletItem = walletItemMapper.toModel(entity)
}
