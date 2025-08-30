package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.models.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.NewBankAccountRequest
import com.ynixt.sharedfinances.domain.repositories.BankAccountRepository
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
        bankAccountRepository.save(
            BankAccount(
                userId = userId,
                balance = newBankAccountRequest.balance,
                enabled = true,
                name = newBankAccountRequest.name,
                currency = newBankAccountRequest.currency,
            ),
        )

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
            ).flatMap { if (it > 0) findBankAccount(id = id, userId = userId) else Mono.empty() }

    @Transactional
    override fun deleteBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        bankAccountRepository
            .deleteByIdAndUserId(
                id = id,
                userId = userId,
            ).map { it > 0 }

    // TODO: criar uma rota para eventos SSE, essa rota deve ser um controller separado, pois qualquer mudança do usuário vai pra ela. Criar uma rota também para mudança do grupo
}
