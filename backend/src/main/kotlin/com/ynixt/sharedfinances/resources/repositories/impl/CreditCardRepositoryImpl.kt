package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.domain.repositories.CreditCardRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.GroupCreditCardR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.CreditCardSpringDataRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class CreditCardRepositoryImpl(
    private val springDataRepository: CreditCardSpringDataRepository,
    private val r2DBCRepository: GroupCreditCardR2DBCRepository,
) : CreditCardRepository {
    override fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<CreditCard> = springDataRepository.findAllByUserId(userId, pageable)

    override fun countByUserId(userId: UUID): Mono<Long> = springDataRepository.countByUserId(userId)

    override fun save(creditCard: CreditCard): Mono<CreditCard> = springDataRepository.save(creditCard)

    override fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<CreditCard> = springDataRepository.findOneByIdAndUserId(id = id, userId = userId)

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
        newTotalLimit: java.math.BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long> =
        springDataRepository.update(
            id = id,
            userId = userId,
            newName = newName,
            newEnabled = newEnabled,
            newCurrency = newCurrency,
            newTotalLimit = newTotalLimit,
            newDueDay = newDueDay,
            newDaysBetweenDueAndClosing = newDaysBetweenDueAndClosing,
            newDueOnNextBusinessDay = newDueOnNextBusinessDay,
        )

    override fun findAllAllowedForGroup(groupId: UUID): Flux<CreditCard> = r2DBCRepository.findAllAllowedForGroup(groupId)

    override fun findAllAssociatedToGroup(groupId: UUID): Flux<CreditCard> = r2DBCRepository.findAllAssociatedToGroup(groupId)
}
