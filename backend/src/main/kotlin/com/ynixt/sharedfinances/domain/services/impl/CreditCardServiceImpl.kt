package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import com.ynixt.sharedfinances.domain.repositories.CreditCardRepository
import com.ynixt.sharedfinances.domain.services.CreditCardService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class CreditCardServiceImpl(
    private val creditCardRepository: CreditCardRepository,
    private val creditCardActionEventService: com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService,
) : CreditCardService {
    override fun findAll(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<CreditCard>> =
        createPage(pageable, countFn = { creditCardRepository.countByUserId(userId) }) {
            creditCardRepository.findAllByUserId(userId, pageable)
        }

    @Transactional
    override fun create(
        userId: UUID,
        request: NewCreditCardRequest,
    ): Mono<CreditCard> =
        creditCardRepository
            .save(
                CreditCard(
                    name = request.name,
                    enabled = true,
                    userId = userId,
                    currency = request.currency,
                    totalLimit = request.totalLimit,
                    availableLimit = request.totalLimit, // on creation, available equals total
                    dueDay = request.dueDay,
                    daysBetweenDueAndClosing = request.daysBetweenDueAndClosing,
                    dueOnNextBusinessDay = request.dueOnNextBusinessDay,
                ),
            ).flatMap { saved ->
                creditCardActionEventService
                    .sendInsertedCreditCard(
                        userId = userId,
                        creditCard = saved,
                    ).thenReturn(saved)
            }

    override fun findOne(
        userId: UUID,
        id: UUID,
    ): Mono<CreditCard> = creditCardRepository.findOneByIdAndUserId(id = id, userId = userId)

    @Transactional
    override fun edit(
        userId: UUID,
        id: UUID,
        request: EditCreditCardRequest,
    ): Mono<CreditCard> =
        creditCardRepository
            .update(
                id = id,
                userId = userId,
                newName = request.newName,
                newEnabled = request.newEnabled,
                newCurrency = request.newCurrency,
                newTotalLimit = request.newTotalLimit,
                newDueDay = request.newDueDay,
                newDaysBetweenDueAndClosing = request.newDaysBetweenDueAndClosing,
                newDueOnNextBusinessDay = request.newDueOnNextBusinessDay,
            ).flatMap { updated ->
                if (updated > 0) {
                    findOne(userId = userId, id = id).flatMap { saved ->
                        creditCardActionEventService
                            .sendUpdatedCreditCard(
                                userId = userId,
                                creditCard = saved,
                            ).thenReturn(saved)
                    }
                } else {
                    Mono.empty()
                }
            }

    @Transactional
    override fun delete(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        creditCardRepository
            .deleteByIdAndUserId(id = id, userId = userId)
            .flatMap { modifiedLines ->
                if (modifiedLines > 0) {
                    creditCardActionEventService
                        .sendDeletedCreditCard(
                            userId = userId,
                            id = id,
                        ).thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }
}
