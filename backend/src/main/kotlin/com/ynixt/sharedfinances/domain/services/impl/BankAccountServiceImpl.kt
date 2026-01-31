package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.bankaccount.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.BankAccountService
import com.ynixt.sharedfinances.domain.services.actionevents.BankAccountActionEventService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BankAccountServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val bankAccountActionEventService: BankAccountActionEventService,
    private val bankAccountMapper: BankAccountMapper,
) : BankAccountService {
    override suspend fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Page<BankAccount> =
        createPage(pageable, countFn = { walletItemRepository.countByUserIdAndType(userId, WalletItemType.BANK_ACCOUNT) }) {
            walletItemRepository
                .findAllByUserIdAndType(
                    userId,
                    WalletItemType.BANK_ACCOUNT,
                    pageable,
                ).map(bankAccountMapper::toModel)
        }

    @Transactional
    override suspend fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): BankAccount =
        walletItemRepository
            .save(
                bankAccountMapper.toEntity(
                    BankAccount(
                        userId = userId,
                        balance = newBankAccountRequest.balance,
                        enabled = true,
                        name = newBankAccountRequest.name,
                        currency = newBankAccountRequest.currency,
                    ),
                ),
            ).awaitSingle()
            .let { saved ->
                bankAccountMapper.toModel(saved).also { savedModel ->
                    bankAccountActionEventService
                        .sendInsertedBankAccount(
                            bankAccount = savedModel,
                            userId = userId,
                        )
                }
            }

    override suspend fun findBankAccount(
        userId: UUID,
        id: UUID,
    ): BankAccount? =
        walletItemRepository
            .findOneByIdAndUserId(
                id = id,
                userId = userId,
            ).awaitSingleOrNull()
            ?.let(bankAccountMapper::toModel)

    @Transactional
    override suspend fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): BankAccount? =
        walletItemRepository
            .updateBankAccount(
                id = id,
                userId = userId,
                newName = editBankAccount.newName,
                newEnabled = editBankAccount.newEnabled,
                newCurrency = editBankAccount.newCurrency,
            ).awaitSingle()
            .let { modifiedLines ->
                if (modifiedLines > 0) {
                    findBankAccount(id = id, userId = userId)?.also { saved ->
                        bankAccountActionEventService
                            .sendInsertedBankAccount(
                                bankAccount = saved,
                                userId = userId,
                            )
                    }
                } else {
                    null
                }
            }

    @Transactional
    override suspend fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Boolean =
        // TODO: only delete if has no wallet entry. Otherwise only disable.
        walletItemRepository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).awaitSingle()
            .let { modifiedLines ->
                (modifiedLines > 0).also {
                    bankAccountActionEventService
                        .sendDeletedBankAccount(
                            id = id,
                            userId = userId,
                        )
                }
            }
}
