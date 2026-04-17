package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardService
import com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreditCardServiceImpl(
    private val walletItemRepository: WalletItemRepository,
    private val walletEventRepository: WalletEventRepository,
    private val recurrenceEventRepository: RecurrenceEventRepository,
    private val creditCardActionEventService: CreditCardActionEventService,
    private val creditCardMapper: CreditCardMapper,
) : CreditCardService {
    override suspend fun findAll(
        userId: UUID,
        pageable: Pageable,
        query: String?,
    ): Page<CreditCard> =
        query
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { normalizedQuery ->
                createPage(
                    pageable,
                    countFn = {
                        walletItemRepository.countByUserIdAndTypeAndNameContainingIgnoreCase(
                            userId = userId,
                            type = WalletItemType.CREDIT_CARD,
                            name = normalizedQuery,
                        )
                    },
                ) {
                    walletItemRepository
                        .findAllByUserIdAndTypeAndNameContainingIgnoreCase(
                            userId = userId,
                            type = WalletItemType.CREDIT_CARD,
                            name = normalizedQuery,
                            pageable = pageable,
                        ).map(creditCardMapper::toModel)
                }
            } ?: createPage(pageable, countFn = { walletItemRepository.countByUserIdAndType(userId, WalletItemType.CREDIT_CARD) }) {
            walletItemRepository.findAllByUserIdAndType(userId, WalletItemType.CREDIT_CARD, pageable).map(creditCardMapper::toModel)
        }

    @Transactional
    override suspend fun create(
        userId: UUID,
        request: NewCreditCardRequest,
    ): CreditCard {
        val saved =
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
                            showOnDashboard = request.showOnDashboard,
                        ),
                    ),
                ).awaitSingle()

        val savedModel = creditCardMapper.toModel(saved)

        creditCardActionEventService
            .sendInsertedCreditCard(
                userId = userId,
                creditCard = savedModel,
            )

        return savedModel
    }

    override suspend fun findOne(
        userId: UUID,
        id: UUID,
    ): CreditCard? = walletItemRepository.findOneByIdAndUserId(id = id, userId = userId).awaitSingleOrNull()?.let(creditCardMapper::toModel)

    @Transactional
    override suspend fun edit(
        userId: UUID,
        id: UUID,
        request: EditCreditCardRequest,
    ): CreditCard? {
        val updated =
            walletItemRepository
                .updateCreditCard(
                    id = id,
                    userId = userId,
                    newName = request.newName,
                    newEnabled = request.newEnabled,
                    newCurrency = request.newCurrency,
                    newShowOnDashboard = request.newShowOnDashboard,
                    newTotalLimit = request.newTotalLimit,
                    newDueDay = request.newDueDay,
                    newDaysBetweenDueAndClosing = request.newDaysBetweenDueAndClosing,
                    newDueOnNextBusinessDay = request.newDueOnNextBusinessDay,
                ).awaitSingle()

        return if (updated > 0) {
            findOne(userId = userId, id = id)?.also {
                creditCardActionEventService
                    .sendUpdatedCreditCard(
                        userId = userId,
                        creditCard = it,
                    )
            }
        } else {
            null
        }
    }

    @Transactional
    override suspend fun delete(
        userId: UUID,
        id: UUID,
    ): Boolean {
        // TODO: only delete if has no wallet entry. Otherwise only disable.
        walletEventRepository
            .deleteAllByWalletItemIdAndUserId(
                walletItemId = id,
                userId = userId,
            ).awaitSingle()

        recurrenceEventRepository
            .deleteAllByWalletItemIdAndUserId(
                walletItemId = id,
                userId = userId,
            ).awaitSingle()

        val modifiedLines =
            walletItemRepository
                .deleteByIdAndUserId(id = id, userId = userId)
                .awaitSingle()

        return (modifiedLines > 0).also { deleted ->
            if (deleted) {
                creditCardActionEventService
                    .sendDeletedCreditCard(
                        userId = userId,
                        id = id,
                    )
            }
        }
    }
}
