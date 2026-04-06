package com.ynixt.sharedfinances.support.util

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.util.UUID

class BankAccountTestUtil(
    private val walletItemRepository: WalletItemRepository,
    private val name: String = "bank-account-it-${UUID.randomUUID()}",
    private val balance: BigDecimal = BigDecimal("1000.00"),
    private val currency: String = "BRL",
    private val enabled: Boolean = true,
) {
    fun createBankAccountOnDatabase(userId: UUID): WalletItemEntity =
        runBlocking {
            walletItemRepository
                .save(
                    WalletItemEntity(
                        type = WalletItemType.BANK_ACCOUNT,
                        name = name,
                        enabled = enabled,
                        userId = userId,
                        currency = currency,
                        balance = balance,
                        totalLimit = null,
                        dueDay = null,
                        daysBetweenDueAndClosing = null,
                        dueOnNextBusinessDay = null,
                    ),
                ).awaitSingle()
        }
}
