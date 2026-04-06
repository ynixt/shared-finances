package com.ynixt.sharedfinances.support.util

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.UUID

class CreditCardTestUtil(
    private val walletItemRepository: WalletItemRepository,
    private val name: String = "credit-card-it-${UUID.randomUUID()}",
    private val balance: BigDecimal = BigDecimal("3000.00"),
    private val currency: String = "BRL",
    private val enabled: Boolean = true,
) {
    fun createCreditCardOnDatabase(userId: UUID): WalletItemEntity =
        runBlocking {
            walletItemRepository
                .save(
                    WalletItemEntity(
                        type = WalletItemType.CREDIT_CARD,
                        name = "Cartao Principal",
                        enabled = true,
                        userId = userId,
                        currency = "BRL",
                        balance = balance,
                        totalLimit = balance,
                        dueDay = 12,
                        daysBetweenDueAndClosing = 7,
                        dueOnNextBusinessDay = true,
                    ),
                ).awaitSingle()
        }
}
