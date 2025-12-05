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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class BankAccountServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val bankAccountActionEventService: BankAccountActionEventService,
    private val bankAccountMapper: BankAccountMapper,
) : BankAccountService {
    override fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<BankAccount>> =
        createPage(pageable, countFn = { walletItemRepository.countByUserIdAndType(userId, WalletItemType.BANK_ACCOUNT) }) {
            walletItemRepository
                .findAllByUserIdAndType(
                    userId,
                    WalletItemType.BANK_ACCOUNT,
                    pageable,
                ).map(bankAccountMapper::toModel)
        }

    @Transactional
    override fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): Mono<BankAccount> =
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
            ).flatMap { saved ->
                bankAccountMapper.toModel(saved).let { savedModel ->
                    bankAccountActionEventService
                        .sendInsertedBankAccount(
                            bankAccount = savedModel,
                            userId = userId,
                        ).thenReturn(savedModel)
                }
            }

    override fun findBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<BankAccount> =
        walletItemRepository
            .findOneByIdAndUserId(
                id = id,
                userId = userId,
            ).map(bankAccountMapper::toModel)

    @Transactional
    override fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): Mono<BankAccount> =
        walletItemRepository
            .updateBankAccount(
                id = id,
                userId = userId,
                newName = editBankAccount.newName,
                newEnabled = editBankAccount.newEnabled,
                newCurrency = editBankAccount.newCurrency,
            ).flatMap {
                if (it > 0) {
                    findBankAccount(id = id, userId = userId).flatMap { saved ->
                        bankAccountActionEventService
                            .sendInsertedBankAccount(
                                bankAccount = saved,
                                userId = userId,
                            ).thenReturn(saved)
                    }
                } else {
                    Mono.empty()
                }
            }

    @Transactional
    override fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        walletItemRepository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).flatMap { modifiedLines ->
                if (modifiedLines > 0) {
                    // TODO: only delete if has no wallet entry. Otherwise only disable.
                    bankAccountActionEventService
                        .sendDeletedBankAccount(
                            id = id,
                            userId = userId,
                        ).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }
}
