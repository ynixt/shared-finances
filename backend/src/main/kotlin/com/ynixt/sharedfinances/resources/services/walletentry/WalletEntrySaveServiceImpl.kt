package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.OriginNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TargetNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TransferTargetValueRequiredException
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

abstract class WalletEntrySaveServiceImpl(
    protected val groupService: GroupService,
    protected val walletItemService: WalletItemService,
    protected val creditCardBillService: CreditCardBillService,
    protected val recurrenceService: RecurrenceService,
    protected val recurrenceEventRepository: RecurrenceEventRepository,
    protected val recurrenceSeriesRepository: RecurrenceSeriesRepository,
    protected val recurrenceEntryRepository: RecurrenceEntryRepository,
    protected val clock: Clock,
) {
    protected suspend fun prepareMutationRequest(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): NewEntryRequest = loadRelationships(userId, newEntryRequest).also(::checkDataIntegrity)

    protected fun hasMutationPermission(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Boolean = checkAllPermissions(userId, newEntryRequest)

    protected suspend fun loadRelationships(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): NewEntryRequest =
        newEntryRequest
            .attachGroup(userId)
            .attachOrigin()
            .attachTarget()
            .attachOriginBill()
            .attachTargetBill()

    protected fun checkDataIntegrity(newEntryRequest: NewEntryRequest) {
        requireNotNull(newEntryRequest.origin)

        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            val origin = newEntryRequest.origin
            val target = requireNotNull(newEntryRequest.target)
            val originValue = requireNotNull(newEntryRequest.transferOriginValue)
            require(originValue > BigDecimal.ZERO)
            val targetValue = newEntryRequest.transferTargetValue
            targetValue?.let { require(it > BigDecimal.ZERO) }

            val sameCurrency = origin.currency == target.currency
            if (newEntryRequest.paymentType == PaymentType.UNIQUE && !sameCurrency && targetValue == null) {
                throw TransferTargetValueRequiredException(
                    fromCurrency = origin.currency,
                    toCurrency = target.currency,
                )
            }
        } else {
            requireNotNull(newEntryRequest.value)
        }

        if (newEntryRequest.origin.type == WalletItemType.CREDIT_CARD) {
            requireNotNull(newEntryRequest.originBill)
        }

        if (newEntryRequest.target?.type == WalletItemType.CREDIT_CARD) {
            requireNotNull(newEntryRequest.targetBill)
        }

        if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
            requireNotNull(newEntryRequest.installments)
            requireNotNull(newEntryRequest.periodicity)
        }

        if (newEntryRequest.paymentType == PaymentType.RECURRING) {
            requireNotNull(newEntryRequest.periodicity)
        }
    }

    protected fun checkAllPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Boolean {
        val hasGroupPermission = newEntryRequest.group == null || newEntryRequest.group.permissions.contains(GroupPermissions.SEND_ENTRIES)
        val hasOriginPermission: Boolean
        val hasTargetPermission: Boolean
        val hasCategoryPermission: Boolean

        if (newEntryRequest.group == null) {
            hasOriginPermission = newEntryRequest.origin!!.userId == userId
            hasTargetPermission = newEntryRequest.target == null || newEntryRequest.target.userId == userId
            hasCategoryPermission = newEntryRequest.category == null || newEntryRequest.category.userId == userId
        } else {
            hasOriginPermission = newEntryRequest.group.itemsAssociatedIds.contains(newEntryRequest.origin!!.id!!)
            hasTargetPermission =
                newEntryRequest.target == null ||
                newEntryRequest.group.itemsAssociatedIds.contains(newEntryRequest.target.id!!)
            hasCategoryPermission = newEntryRequest.category == null || newEntryRequest.category.groupId == newEntryRequest.group.id
        }

        return hasGroupPermission && hasOriginPermission && hasTargetPermission && hasCategoryPermission
    }

    protected fun requestToEventEntity(
        id: UUID?,
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        recurrenceConfig: RecurrenceEventEntity?,
        installment: Int?,
        date: LocalDate = newEntryRequest.date,
    ): WalletEventEntity =
        WalletEventEntity(
            type = newEntryRequest.type,
            userId = if (newEntryRequest.groupId == null) userId else null,
            groupId = newEntryRequest.groupId,
            name = newEntryRequest.name,
            categoryId = newEntryRequest.categoryId,
            date = date,
            confirmed = newEntryRequest.confirmed,
            observations = newEntryRequest.observations,
            tags = newEntryRequest.tags?.ifEmpty { null },
            installment = installment,
            recurrenceEventId = recurrenceConfig?.id,
            paymentType = newEntryRequest.paymentType,
            initialBalance = newEntryRequest.initialBalance,
        ).also {
            it.id = id
        }

    protected fun resolveTransferOriginValue(newEntryRequest: NewEntryRequest): BigDecimal =
        requireNotNull(newEntryRequest.transferOriginValue)

    protected fun resolveConcreteTransferTargetValue(newEntryRequest: NewEntryRequest): BigDecimal {
        val originValue = resolveTransferOriginValue(newEntryRequest)
        val targetValue = newEntryRequest.transferTargetValue
        if (targetValue != null) {
            return targetValue
        }

        val originCurrency = newEntryRequest.origin?.currency
        val targetCurrency = newEntryRequest.target?.currency
        val sameCurrency = originCurrency == targetCurrency
        if (!sameCurrency) {
            throw TransferTargetValueRequiredException(
                fromCurrency = originCurrency ?: "unknown",
                toCurrency = targetCurrency ?: "unknown",
            )
        }
        return originValue
    }

    protected fun resolveTemplateTransferTargetValue(newEntryRequest: NewEntryRequest): BigDecimal =
        resolveTransferOriginValue(newEntryRequest)

    protected fun requestToEntryEntity(
        id: UUID?,
        event: WalletEventEntity,
        newEntryRequest: NewEntryRequest,
    ): List<WalletEntryEntity> {
        val entries =
            mutableListOf(
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value =
                        if (newEntryRequest.type ==
                            WalletEntryType.TRANSFER
                        ) {
                            resolveTransferOriginValue(newEntryRequest).unaryMinus()
                        } else {
                            requireNotNull(newEntryRequest.valueFixedForType)
                        },
                    walletItemId = newEntryRequest.originId,
                    billId = newEntryRequest.originBill?.id,
                ).also {
                    it.id = id
                },
            )

        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            entries.add(
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value = resolveConcreteTransferTargetValue(newEntryRequest),
                    walletItemId = newEntryRequest.targetId!!,
                    billId = newEntryRequest.targetBill?.id,
                ).also {
                    it.id = id
                },
            )
        }

        return entries
    }

    protected suspend fun requestToRecurrenceEntity(
        id: UUID?,
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        qtyLimit: Int?,
        seriesId: UUID? = null,
        seriesQtyTotal: Int? = null,
        seriesOffset: Int = 0,
    ): RecurrenceEventEntity {
        val periodicity = newEntryRequest.periodicity ?: RecurrenceType.SINGLE

        val now = LocalDate.now(clock)
        val alreadyExecuted = !newEntryRequest.isInFuture(now)
        val qtyExecuted = if (alreadyExecuted) 1 else 0

        val lastExecution: LocalDate? = if (alreadyExecuted) now else null

        val requestOriginBillDate = newEntryRequest.originBill?.billDate
        val requestTargetBillDate = newEntryRequest.targetBill?.billDate

        val nextOriginBillDate =
            if (requestOriginBillDate == null) {
                null
            } else if (alreadyExecuted) {
                recurrenceService.calculateNextExecution(requestOriginBillDate, periodicity, qtyExecuted, qtyLimit)
            } else {
                requestOriginBillDate
            }

        val nextTargetBillDate =
            if (requestTargetBillDate == null) {
                null
            } else if (alreadyExecuted) {
                recurrenceService.calculateNextExecution(requestTargetBillDate, periodicity, qtyExecuted, qtyLimit)
            } else {
                requestTargetBillDate
            }

        val resolvedSeriesQtyTotal =
            if (seriesId == null) {
                seriesQtyTotal ?: resolveInitialSeriesQtyTotal(
                    paymentType = newEntryRequest.paymentType,
                    qtyLimit = qtyLimit,
                    qtyExecuted = qtyExecuted,
                )
            } else {
                seriesQtyTotal ?: recurrenceSeriesRepository.findById(seriesId).awaitSingleOrNull()?.qtyTotal
            }

        val resolvedSeriesId =
            seriesId
                ?: recurrenceSeriesRepository
                    .save(
                        RecurrenceSeriesEntity(
                            qtyTotal = resolvedSeriesQtyTotal,
                        ),
                    ).awaitSingle()
                    .id!!

        val recurrenceToPersist =
            RecurrenceEventEntity(
                name = newEntryRequest.name,
                categoryId = newEntryRequest.categoryId,
                userId = userId,
                groupId = newEntryRequest.groupId,
                tags = newEntryRequest.tags?.ifEmpty { null },
                observations = newEntryRequest.observations,
                type = newEntryRequest.type,
                periodicity = periodicity,
                paymentType = newEntryRequest.paymentType,
                qtyExecuted = qtyExecuted,
                qtyLimit = qtyLimit,
                lastExecution = lastExecution,
                nextExecution =
                    if (alreadyExecuted) {
                        recurrenceService.calculateNextExecution(
                            lastExecution = lastExecution!!,
                            periodicity = periodicity,
                            qtyExecuted = qtyExecuted,
                            qtyLimit = qtyLimit,
                        )
                    } else {
                        newEntryRequest.date
                    },
                endExecution =
                    recurrenceService.calculateEndDate(
                        lastExecution = lastExecution ?: newEntryRequest.date,
                        periodicity = periodicity,
                        qtyExecuted = qtyExecuted,
                        qtyLimit = qtyLimit?.let { if (lastExecution == null) it - 1 else it },
                    ),
                seriesId = resolvedSeriesId,
                seriesOffset = seriesOffset,
            ).also {
                if (id != null) {
                    it.id = id
                } else {
                    // Preserve createdAt through in-memory copies used later in the same transaction.
                    it.createdAt = OffsetDateTime.now(clock)
                }
            }

        return recurrenceEventRepository
            .save(recurrenceToPersist)
            .awaitSingle()
            .also { savedRecurrence ->
                savedRecurrence.seriesQtyTotal = resolvedSeriesQtyTotal
                val entries = mutableListOf<RecurrenceEntryEntity>()

                entries.add(
                    RecurrenceEntryEntity(
                        value =
                            if (newEntryRequest.type ==
                                WalletEntryType.TRANSFER
                            ) {
                                resolveTransferOriginValue(newEntryRequest).unaryMinus()
                            } else {
                                requireNotNull(newEntryRequest.valueFixedForType)
                            },
                        walletEventId = savedRecurrence.id!!,
                        walletItemId = newEntryRequest.originId,
                        nextBillDate = nextOriginBillDate,
                        lastBillDate =
                            if (requestOriginBillDate == null) {
                                null
                            } else {
                                recurrenceService
                                    .calculateEndDate(
                                        lastExecution = requestOriginBillDate,
                                        periodicity = periodicity,
                                        qtyExecuted = qtyExecuted,
                                        qtyLimit = qtyLimit?.let { if (alreadyExecuted) it else it - 1 },
                                    )?.withStartOfMonth()
                            },
                    ),
                )

                if (newEntryRequest.type == WalletEntryType.TRANSFER) {
                    entries.add(
                        RecurrenceEntryEntity(
                            value = resolveTemplateTransferTargetValue(newEntryRequest),
                            walletEventId = savedRecurrence.id!!,
                            walletItemId = newEntryRequest.targetId!!,
                            nextBillDate = nextTargetBillDate,
                            lastBillDate =
                                if (nextTargetBillDate == null) {
                                    null
                                } else {
                                    recurrenceService
                                        .calculateEndDate(
                                            lastExecution = nextTargetBillDate,
                                            periodicity = periodicity,
                                            qtyExecuted = qtyExecuted,
                                            qtyLimit = qtyLimit?.let { if (alreadyExecuted) it else it - 1 },
                                        )?.withStartOfMonth()
                                },
                        ),
                    )
                }

                savedRecurrence.entries =
                    recurrenceEntryRepository.saveAll(entries).asFlow().toList().also { entries ->
                        entries.forEach { entry ->
                            entry.event = savedRecurrence
                        }
                    }
            }
    }

    private fun resolveInitialSeriesQtyTotal(
        paymentType: PaymentType,
        qtyLimit: Int?,
        qtyExecuted: Int,
    ): Int? =
        when (paymentType) {
            PaymentType.INSTALLMENTS -> qtyLimit
            PaymentType.RECURRING -> qtyLimit ?: qtyExecuted
            else -> null
        }

    private suspend fun <ID, ENTITY : Any> NewEntryRequest.attachRequired(
        id: ID,
        fetch: suspend (ID) -> ENTITY?,
        onFound: NewEntryRequest.(ENTITY) -> NewEntryRequest,
        onNotFound: (ID) -> Throwable,
    ): NewEntryRequest =
        fetch(id)
            ?.let { entity -> onFound(entity) } ?: throw onNotFound(id)

    private suspend fun <ID, ENTITY : Any> NewEntryRequest.attachOptional(
        id: ID?,
        fetch: suspend (ID) -> ENTITY?,
        onFound: NewEntryRequest.(ENTITY) -> NewEntryRequest,
        onNotFound: (ID) -> Throwable,
    ): NewEntryRequest =
        if (id != null) {
            fetch(id)
                ?.let { entity -> onFound(entity) } ?: throw onNotFound(id)
        } else {
            this
        }

    private suspend fun NewEntryRequest.attachGroup(userId: UUID): NewEntryRequest =
        attachOptional(
            id = groupId,
            fetch = { groupId -> groupService.findGroupWithAssociatedItems(userId, groupId) },
            onFound = { group -> copy(group = group) },
            onNotFound = { groupId -> GroupNotFoundException(groupId) },
        )

    private suspend fun NewEntryRequest.attachOrigin(): NewEntryRequest =
        attachRequired(
            id = originId,
            fetch = { originId -> walletItemService.findOne(originId) },
            onFound = { origin -> copy(origin = origin) },
            onNotFound = { originId -> OriginNotFoundException(originId) },
        )

    private suspend fun NewEntryRequest.attachTarget(): NewEntryRequest =
        attachOptional(
            id = targetId,
            fetch = { targetId -> walletItemService.findOne(targetId) },
            onFound = { target -> copy(target = target) },
            onNotFound = { targetId -> TargetNotFoundException(targetId) },
        )

    private suspend fun NewEntryRequest.attachOriginBill(): NewEntryRequest =
        attachBillIfNeeded(
            creditCard = origin as? CreditCard,
            billDate = originBillDate,
        ) { bill ->
            copy(originBill = bill)
        }

    private suspend fun NewEntryRequest.attachTargetBill(): NewEntryRequest =
        attachBillIfNeeded(
            creditCard = target as? CreditCard,
            billDate = targetBillDate,
        ) { bill ->
            copy(targetBill = bill)
        }

    private suspend fun NewEntryRequest.attachBillIfNeeded(
        creditCard: CreditCard?,
        billDate: LocalDate?,
        setBill: NewEntryRequest.(CreditCardBillEntity) -> NewEntryRequest,
    ): NewEntryRequest =
        attachOptional(
            id = if (creditCard != null && billDate != null) creditCard to billDate else null,
            fetch = { (card, billDate) ->
                val dueDate = card.getDueDate(billDate)
                creditCardBillService.getOrCreateBill(
                    creditCardId = requireNotNull(card.id),
                    dueDate = dueDate,
                    closingDate = card.getClosingDate(dueDate),
                )
            },
            onFound = { bill -> setBill(bill) },
            onNotFound = { _ -> IllegalStateException("Unexpected empty bill creation result") },
        )
}
