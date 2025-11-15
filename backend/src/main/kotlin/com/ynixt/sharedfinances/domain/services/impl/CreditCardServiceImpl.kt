package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardService
import com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class CreditCardServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val creditCardActionEventService: CreditCardActionEventService,
    private val creditCardMapper: CreditCardMapper,
) : CreditCardService {
    override fun findAll(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<CreditCard>> =
        createPage(pageable, countFn = { walletItemRepository.countByUserIdAndType(userId, WalletItemType.CREDIT_CARD) }) {
            walletItemRepository.findAllByUserIdAndType(userId, WalletItemType.CREDIT_CARD, pageable).map(creditCardMapper::toModel)
        }

    @Transactional
    override fun create(
        userId: UUID,
        request: NewCreditCardRequest,
    ): Mono<CreditCard> =
        walletItemRepository
            .save(
                creditCardMapper.toEntity(
                    CreditCard(
                        name = request.name,
                        enabled = true,
                        userId = userId,
                        currency = request.currency,
                        totalLimit = request.totalLimit,
                        balance = request.totalLimit, // on creation, available equals total
                        dueDay = request.dueDay,
                        daysBetweenDueAndClosing = request.daysBetweenDueAndClosing,
                        dueOnNextBusinessDay = request.dueOnNextBusinessDay,
                    ),
                ),
            ).flatMap { saved ->
                creditCardMapper.toModel(saved).let { savedModel ->
                    creditCardActionEventService
                        .sendInsertedCreditCard(
                            userId = userId,
                            creditCard = savedModel,
                        ).thenReturn(savedModel)
                }
            }

    override fun findOne(
        userId: UUID,
        id: UUID,
    ): Mono<CreditCard> = walletItemRepository.findOneByIdAndUserId(id = id, userId = userId).map(creditCardMapper::toModel)

    @Transactional
    override fun edit(
        userId: UUID,
        id: UUID,
        request: EditCreditCardRequest,
    ): Mono<CreditCard> =
        walletItemRepository
            .updateCreditCard(
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
        walletItemRepository
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
