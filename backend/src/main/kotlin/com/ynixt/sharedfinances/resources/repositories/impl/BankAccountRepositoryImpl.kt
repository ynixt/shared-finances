package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.repositories.BankAccountRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.GroupBankAccountR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.BankAccountSpringDataRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class BankAccountRepositoryImpl(
    private val springDataRepository: BankAccountSpringDataRepository,
    private val r2DBCRepository: GroupBankAccountR2DBCRepository,
) : BankAccountRepository {
    override fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<BankAccount> = springDataRepository.findAllByUserId(userId, pageable)

    override fun countByUserId(userId: UUID): Mono<Long> = springDataRepository.countByUserId(userId)

    override fun save(bankAccount: BankAccount): Mono<BankAccount> = springDataRepository.save(bankAccount)

    override fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<BankAccount> = springDataRepository.findOneByIdAndUserId(id = id, userId = userId)

    override fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long> = springDataRepository.deleteByIdAndUserId(id = id, userId = userId)

    override fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
    ): Mono<Long> =
        springDataRepository.update(id = id, userId = userId, newName = newName, newEnabled = newEnabled, newCurrency = newCurrency)

    override fun findAllAllowedForGroup(groupId: UUID): Flux<BankAccount> = r2DBCRepository.findAllAllowedForGroup(groupId)

    override fun findAllAssociatedToGroup(groupId: UUID): Flux<BankAccount> = r2DBCRepository.findAllAssociatedToGroup(groupId)
}
