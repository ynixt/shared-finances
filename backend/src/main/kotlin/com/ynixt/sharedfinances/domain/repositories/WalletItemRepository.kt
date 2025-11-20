package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

interface WalletItemRepository {
    fun findAllByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemEntity>

    fun countByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
    ): Mono<Long>

    fun findAllByUserIdAndType(
        userId: UUID,
        type: WalletItemType,
        pageable: Pageable,
    ): Flux<WalletItemEntity>

    fun countByUserIdAndType(
        userId: UUID,
        type: WalletItemType,
    ): Mono<Long>

    fun save(walletItem: WalletItemEntity): Mono<WalletItemEntity>

    fun findOneById(id: UUID): Mono<WalletItemEntity>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<WalletItemEntity>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun updateBankAccount(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
    ): Mono<Long>

    fun updateCreditCard(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newTotalLimit: BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long>

    fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Mono<Long>
}
