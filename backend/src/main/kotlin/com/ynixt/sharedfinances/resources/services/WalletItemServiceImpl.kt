package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
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
        onlyBankAccounts: Boolean,
        query: String?,
    ): Page<WalletItem> {
        val normalizedQuery = query?.trim()?.takeIf(String::isNotEmpty)
        return if (onlyBankAccounts && normalizedQuery != null) {
            createPage(
                pageable,
                countFn = {
                    repository.countByUserIdAndTypeAndNameContainingIgnoreCase(userId, WalletItemType.BANK_ACCOUNT, normalizedQuery)
                },
            ) {
                repository
                    .findAllByUserIdAndTypeAndNameContainingIgnoreCase(
                        userId = userId,
                        type = WalletItemType.BANK_ACCOUNT,
                        name = normalizedQuery,
                        pageable = pageable,
                    ).map(this::convert)
            }
        } else if (onlyBankAccounts) {
            createPage(pageable, countFn = { repository.countByUserIdAndType(userId, WalletItemType.BANK_ACCOUNT) }) {
                repository
                    .findAllByUserIdAndType(
                        userId = userId,
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = pageable,
                    ).map(this::convert)
            }
        } else if (normalizedQuery != null) {
            createPage(
                pageable,
                countFn = { repository.countByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, normalizedQuery) },
            ) {
                repository
                    .findAllByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, normalizedQuery, pageable)
                    .map(this::convert)
            }
        } else {
            createPage(pageable, countFn = { repository.countByUserIdAndEnabled(userId, enabled = true) }) {
                repository
                    .findAllByUserIdAndEnabled(
                        userId = userId,
                        enabled = true,
                        pageable = pageable,
                    ).map(this::convert)
            }
        }
    }

    override suspend fun findOne(id: UUID): WalletItem? = repository.findOneById(id).map(this::convert).awaitSingleOrNull()

    override suspend fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Long = repository.addBalanceById(id, balance).awaitSingle()

    override fun convert(entity: WalletItemEntity): WalletItem = walletItemMapper.toModel(entity)
}
