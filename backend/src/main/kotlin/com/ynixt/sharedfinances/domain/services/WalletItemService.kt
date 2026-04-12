package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.WalletItem
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.util.UUID

interface WalletItemService {
    suspend fun findAllItems(
        userId: UUID,
        pageable: Pageable,
        onlyBankAccounts: Boolean = false,
    ): Page<WalletItem>

    suspend fun findOne(id: UUID): WalletItem?

    fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletItem>

    suspend fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Long
}
