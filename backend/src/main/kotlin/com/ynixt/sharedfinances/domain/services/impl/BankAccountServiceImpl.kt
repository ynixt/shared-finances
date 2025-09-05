package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.NewBankAccountRequest
import com.ynixt.sharedfinances.domain.repositories.BankAccountRepository
import com.ynixt.sharedfinances.domain.services.ActionEventService
import com.ynixt.sharedfinances.domain.services.BankAccountService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class BankAccountServiceImpl(
    private val bankAccountRepository: BankAccountRepository,
    private val actionEventService: ActionEventService,
) : BankAccountService {
    override fun findAllBanks(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<BankAccount>> =
        createPage(pageable, countFn = { bankAccountRepository.countByUserId(userId) }) {
            bankAccountRepository.findAllByUserId(
                userId,
                pageable,
            )
        }

    @Transactional
    override fun newBankAccount(
        userId: UUID,
        newBankAccountRequest: NewBankAccountRequest,
    ): Mono<BankAccount> =
        bankAccountRepository
            .save(
                BankAccount(
                    userId = userId,
                    balance = newBankAccountRequest.balance,
                    enabled = true,
                    name = newBankAccountRequest.name,
                    currency = newBankAccountRequest.currency,
                ),
            ).flatMap { saved ->
                actionEventService
                    .newEvent(
                        data = saved,
                        userId = userId,
                        type = ActionEventType.INSERT,
                        category = ActionEventCategory.BANK_ACCOUNT,
                    ).thenReturn(saved)
            }

    override fun findBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<BankAccount> =
        bankAccountRepository.findOneByIdAndUserId(
            id = id,
            userId = userId,
        )

    @Transactional
    override fun editBankAccount(
        userId: UUID,
        id: UUID,
        editBankAccount: EditBankAccountRequest,
    ): Mono<BankAccount> =
        bankAccountRepository
            .update(
                id = id,
                userId = userId,
                newName = editBankAccount.newName,
                newEnabled = editBankAccount.newEnabled,
                newCurrency = editBankAccount.newCurrency,
            ).flatMap {
                if (it > 0) {
                    findBankAccount(id = id, userId = userId).flatMap { saved ->
                        actionEventService
                            .newEvent(
                                data = saved,
                                userId = userId,
                                type = ActionEventType.UPDATE,
                                category = ActionEventCategory.BANK_ACCOUNT,
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
        bankAccountRepository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).flatMap { modifiedLines ->
                if (modifiedLines > 0) {
                    actionEventService
                        .newEvent(
                            data = id,
                            userId = userId,
                            type = ActionEventType.DELETE,
                            category = ActionEventCategory.BANK_ACCOUNT,
                        ).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }
}
