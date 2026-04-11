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
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletSourceSplitException
import com.ynixt.sharedfinances.domain.exceptions.http.MixedCurrencyWalletSourcesException
import com.ynixt.sharedfinances.domain.exceptions.http.OriginNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TargetNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TransferTargetValueRequiredException
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletSourceLeg
import com.ynixt.sharedfinances.domain.models.walletentry.ResolvedWalletSourceLeg
import com.ynixt.sharedfinances.domain.models.walletentry.WalletSourceSplit
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
import java.math.RoundingMode
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
    ): NewEntryRequest {
        val withGroup = newEntryRequest.attachGroup(userId)
        return if (withGroup.type == WalletEntryType.TRANSFER) {
            withGroup
                .attachOriginForTransfer()
                .attachTarget()
                .attachOriginBill()
                .attachTargetBill()
        } else {
            withGroup.attachResolvedSourceLegs()
        }
    }

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
            requireNotNull(newEntryRequest.resolvedSources)
        }

        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            if (newEntryRequest.origin!!.type == WalletItemType.CREDIT_CARD) {
                requireNotNull(newEntryRequest.originBill)
            }
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
            hasOriginPermission =
                if (newEntryRequest.type == WalletEntryType.TRANSFER) {
                    newEntryRequest.origin!!.userId == userId
                } else {
                    newEntryRequest.resolvedSources!!.all { it.walletItem.userId == userId }
                }
            hasTargetPermission = newEntryRequest.target == null || newEntryRequest.target.userId == userId
            hasCategoryPermission = newEntryRequest.category == null || newEntryRequest.category.userId == userId
        } else {
            hasOriginPermission =
                if (newEntryRequest.type == WalletEntryType.TRANSFER) {
                    newEntryRequest.group.itemsAssociatedIds.contains(newEntryRequest.origin!!.id!!)
                } else {
                    newEntryRequest.resolvedSources!!.all { leg ->
                        newEntryRequest.group!!.itemsAssociatedIds.contains(leg.walletItem.id!!)
                    }
                }
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
        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            return listOf(
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value = resolveTransferOriginValue(newEntryRequest).unaryMinus(),
                    walletItemId = requireNotNull(newEntryRequest.originId),
                    billId = newEntryRequest.originBill?.id,
                    contributionPercent = null,
                ).also {
                    it.id = id
                },
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value = resolveConcreteTransferTargetValue(newEntryRequest),
                    walletItemId = newEntryRequest.targetId!!,
                    billId = newEntryRequest.targetBill?.id,
                    contributionPercent = null,
                ),
            )
        }

        val legs = requireNotNull(newEntryRequest.resolvedSources)
        val totalMag = requireNotNull(newEntryRequest.value).abs()
        val percents = legs.map { it.contributionPercent }
        val values = WalletSourceSplit.distributeLegValues(newEntryRequest.type, totalMag, percents)
        return legs.mapIndexed { index, leg ->
            WalletEntryEntity(
                walletEventId = event.id!!,
                value = values[index],
                walletItemId = leg.walletItemId,
                billId = leg.bill?.id,
                contributionPercent = leg.contributionPercent,
            ).also {
                if (index == 0) {
                    it.id = id
                }
            }
        }
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
                    it.createdAt = OffsetDateTime.now(clock)
                }
            }

        return recurrenceEventRepository
            .save(recurrenceToPersist)
            .awaitSingle()
            .also { savedRecurrence ->
                savedRecurrence.seriesQtyTotal = resolvedSeriesQtyTotal
                val entries =
                    if (newEntryRequest.type == WalletEntryType.TRANSFER) {
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

                        listOf(
                            RecurrenceEntryEntity(
                                value = resolveTransferOriginValue(newEntryRequest).unaryMinus(),
                                walletEventId = savedRecurrence.id!!,
                                walletItemId = requireNotNull(newEntryRequest.originId),
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
                                contributionPercent = null,
                            ),
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
                                contributionPercent = null,
                            ),
                        )
                    } else {
                        val legs = requireNotNull(newEntryRequest.resolvedSources)
                        val totalMag = requireNotNull(newEntryRequest.value).abs()
                        val percents = legs.map { it.contributionPercent }
                        val legValues = WalletSourceSplit.distributeLegValues(newEntryRequest.type, totalMag, percents)
                        legs.mapIndexed { index, leg ->
                            val requestBillDate = leg.bill?.billDate
                            val nextBillDate =
                                if (requestBillDate == null) {
                                    null
                                } else if (alreadyExecuted) {
                                    recurrenceService.calculateNextExecution(requestBillDate, periodicity, qtyExecuted, qtyLimit)
                                } else {
                                    requestBillDate
                                }
                            val lastBillDate =
                                if (requestBillDate == null) {
                                    null
                                } else {
                                    recurrenceService
                                        .calculateEndDate(
                                            lastExecution = requestBillDate,
                                            periodicity = periodicity,
                                            qtyExecuted = qtyExecuted,
                                            qtyLimit = qtyLimit?.let { if (alreadyExecuted) it else it - 1 },
                                        )?.withStartOfMonth()
                                }
                            RecurrenceEntryEntity(
                                value = legValues[index],
                                walletEventId = savedRecurrence.id!!,
                                walletItemId = leg.walletItemId,
                                nextBillDate = nextBillDate,
                                lastBillDate = lastBillDate,
                                contributionPercent = leg.contributionPercent,
                            )
                        }
                    }

                savedRecurrence.entries =
                    recurrenceEntryRepository.saveAll(entries).asFlow().toList().also { persisted ->
                        persisted.forEach { entry ->
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

    private suspend fun NewEntryRequest.attachOriginForTransfer(): NewEntryRequest =
        attachRequired(
            id = requireNotNull(originId) { "originId is required for transfer" },
            fetch = { oid -> walletItemService.findOne(oid) },
            onFound = { origin -> copy(origin = origin) },
            onNotFound = { oid -> OriginNotFoundException(oid) },
        )

    private suspend fun NewEntryRequest.attachResolvedSourceLegs(): NewEntryRequest {
        val raw =
            when {
                !sources.isNullOrEmpty() -> sources!!
                originId != null ->
                    listOf(
                        NewWalletSourceLeg(
                            walletItemId = originId!!,
                            contributionPercent = BigDecimal("100.00"),
                            billDate = originBillDate,
                        ),
                    )

                else -> throw InvalidWalletSourceSplitException("sources or originId is required")
            }
        WalletSourceSplit.validatePercentsSumExactly100(raw.map { it.contributionPercent })
        val resolved =
            raw.map { leg ->
                val item =
                    walletItemService.findOne(leg.walletItemId)
                        ?: throw OriginNotFoundException(leg.walletItemId)
                val bill =
                    if (item is CreditCard) {
                        if (leg.billDate == null) {
                            throw InvalidWalletSourceSplitException(
                                "billDate is required for credit card source ${leg.walletItemId}",
                            )
                        }
                        val dueDate = item.getDueDate(leg.billDate)
                        creditCardBillService.getOrCreateBill(
                            creditCardId = requireNotNull(item.id),
                            dueDate = dueDate,
                            closingDate = item.getClosingDate(dueDate),
                        )
                    } else {
                        null
                    }
                ResolvedWalletSourceLeg(
                    walletItemId = leg.walletItemId,
                    contributionPercent = leg.contributionPercent.setScale(2, RoundingMode.HALF_UP),
                    walletItem = item,
                    bill = bill,
                )
            }
        val currencies = resolved.map { it.walletItem.currency }.toSet()
        if (currencies.size > 1) {
            throw MixedCurrencyWalletSourcesException(currencies)
        }
        return copy(
            resolvedSources = resolved,
            origin = resolved.first().walletItem,
            originBill = resolved.first().bill,
            originId = resolved.first().walletItemId,
            sources =
                resolved.map { leg ->
                    NewWalletSourceLeg(
                        walletItemId = leg.walletItemId,
                        contributionPercent = leg.contributionPercent,
                        billDate = leg.bill?.billDate,
                    )
                },
        )
    }

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
