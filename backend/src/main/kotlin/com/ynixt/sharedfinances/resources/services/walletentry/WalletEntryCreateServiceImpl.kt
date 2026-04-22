package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.isSameMonthYear
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.ifEmpty

@Service
class WalletEntryCreateServiceImpl(
    private val walletEventRepository: WalletEventRepository,
    private val walletEntryRepository: WalletEntryRepository,
    groupService: GroupService,
    walletItemService: WalletItemService,
    genericCategoryService: GenericCategoryService,
    categoryConceptService: CategoryConceptService,
    creditCardBillService: CreditCardBillService,
    recurrenceService: RecurrenceService,
    recurrenceEventRepository: RecurrenceEventRepository,
    recurrenceSeriesRepository: RecurrenceSeriesRepository,
    recurrenceEntryRepository: RecurrenceEntryRepository,
    private val groupDebtService: GroupDebtService,
    walletEventBeneficiaryRepository: WalletEventBeneficiarySpringDataRepository,
    recurrenceEventBeneficiaryRepository: RecurrenceEventBeneficiarySpringDataRepository,
    private val walletEventActionEventService: WalletEventActionEventService,
    private val walletItemMapper: WalletItemMapper,
    private val exchangeRateService: ExchangeRateService,
    clock: Clock,
) : WalletEntrySaveServiceImpl(
        groupService = groupService,
        walletItemService = walletItemService,
        genericCategoryService = genericCategoryService,
        categoryConceptService = categoryConceptService,
        creditCardBillService = creditCardBillService,
        recurrenceService = recurrenceService,
        recurrenceEventRepository = recurrenceEventRepository,
        recurrenceSeriesRepository = recurrenceSeriesRepository,
        recurrenceEntryRepository = recurrenceEntryRepository,
        walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
        recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
        clock = clock,
    ),
    WalletEntryCreateService {
    @Transactional
    override suspend fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEventEntity? {
        val preparedRequest = prepareMutationRequest(userId, newEntryRequest)
        ensureGroupMutationPermission(preparedRequest)

        return if (hasMutationPermission(userId, preparedRequest)) {
            createWithoutCheckPermissions(userId, preparedRequest)
                .also { walletEventActionEventService.sendInsertedWalletEvent(userId, it) }
        } else {
            null
        }
    }

    private fun ensureGroupMutationPermission(preparedRequest: NewEntryRequest) {
        if (preparedRequest.groupId != null && preparedRequest.group?.permissions?.contains(GroupPermissions.SEND_ENTRIES) != true) {
            throw UnauthorizedException()
        }
    }

    @Transactional
    override suspend fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): MinimumWalletEventEntity? {
        val event = recurrenceEventRepository.findById(recurrenceConfigId).awaitSingle()
        event.seriesQtyTotal = recurrenceSeriesRepository.findById(event.seriesId).awaitSingleOrNull()?.qtyTotal

        val entries =
            recurrenceEntryRepository.findAllByWalletEventId(recurrenceConfigId).asFlow().toList().also {
                event.entries =
                    it.also {
                        it.forEach { entry ->
                            entry.event = event
                        }
                    }
            }
        val beneficiaries =
            recurrenceEventBeneficiaryRepository
                .findAllByWalletEventId(recurrenceConfigId)
                .asFlow()
                .toList()
                .also { loaded ->
                    event.beneficiaries =
                        loaded.also { items ->
                            items.forEach { beneficiary ->
                                beneficiary.event = event
                            }
                        }
                }

        require(event.entries!!.isNotEmpty())

        val nextExecution =
            recurrenceService
                .calculateNextExecution(
                    lastExecution = date,
                    periodicity = event.periodicity,
                    qtyExecuted = event.qtyExecuted + 1,
                    qtyLimit = event.qtyLimit,
                )

        val linesModified =
            recurrenceEventRepository
                .updateConfigCausedByExecution(
                    id = recurrenceConfigId,
                    oldNextExecution = date,
                    nextExecution = nextExecution,
                ).awaitSingle()

        if (linesModified == 0) {
            return null
        }

        if (event.paymentType == PaymentType.RECURRING && event.qtyLimit == null) {
            recurrenceSeriesRepository.incrementQtyTotal(event.seriesId).awaitSingle()
            event.seriesQtyTotal = (event.seriesQtyTotal ?: 0) + 1
        }

        entries
            .map { entryTemplate ->
                recurrenceEntryRepository.updateNextBillDate(
                    id = entryTemplate.id!!,
                    nextBillDate =
                        if (nextExecution == null || entryTemplate.nextBillDate == null) {
                            null
                        } else {
                            calculateNextBillDate(entryTemplate.nextBillDate, event.periodicity)
                        },
                )
            }.also {
                Flux.merge(it).then().awaitSingleOrNull()
            }

        val installment =
            if (event.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                event.seriesOffset + event.qtyExecuted + 1
            } else {
                null
            }

        val walletEventSaved =
            walletEventRepository
                .save(
                    WalletEventEntity(
                        type = event.type,
                        createdByUserId = event.createdByUserId,
                        groupId = event.groupId,
                        name = event.name,
                        categoryId = event.categoryId,
                        date = date,
                        confirmed = false,
                        observations = event.observations,
                        tags = event.tags?.ifEmpty { null },
                        installment = installment,
                        recurrenceEventId = event.id,
                        paymentType = event.paymentType,
                        transferPurpose = event.transferPurpose,
                    ),
                ).awaitSingle()
                .also {
                    it.recurrenceEvent = event
                }

        val walletItems =
            walletItemService
                .findAllByIdIn(
                    entries
                        .map { entry ->
                            entry.walletItemId
                        }.distinct(),
                ).toList()
        val walletItemsById = walletItems.associateBy { it.id!! }
        val walletItemsForEntries = entries.map { entry -> walletItemsById.getValue(entry.walletItemId) }
        val resolvedTransferValuesByWalletItemId =
            if (event.type == WalletEntryType.TRANSFER) {
                resolveMaterializedTransferValues(entries, walletItemsById, date)
            } else {
                emptyMap()
            }

        val bills =
            entries.map { entry ->
                val walletItem = walletItemsById.getValue(entry.walletItemId)

                if (walletItem is CreditCard) {
                    val billDate =
                        requireNotNull(entry.nextBillDate) {
                            "Recurring credit card entry ${entry.id} must define nextBillDate"
                        }
                    val dueDate = walletItem.getDueDate(billDate)
                    creditCardBillService
                        .getOrCreateBill(
                            creditCardId = walletItem.id!!,
                            dueDate = dueDate,
                            closingDate = walletItem.getClosingDate(dueDate),
                        )
                } else {
                    null
                }
            }

        walletEventSaved.entries =
            walletEntryRepository
                .saveAll(
                    entries.mapIndexed { index, entryTemplate ->
                        WalletEntryEntity(
                            walletEventId = walletEventSaved.id!!,
                            value = resolvedTransferValuesByWalletItemId[entryTemplate.walletItemId] ?: entryTemplate.value,
                            walletItemId = walletItemsForEntries[index].id!!,
                            billId = bills[index]?.id,
                            contributionPercent = entryTemplate.contributionPercent,
                        )
                    },
                ).asFlow()
                .toList()
                .also { entries ->
                    entries.forEachIndexed { index, saved ->
                        saved.walletItem = walletItemMapper.fromModel(walletItemsForEntries[index])
                        saved.bill = bills[index]
                        saved.event = walletEventSaved

                        updateBalance(saved)
                    }
                }

        walletEventSaved.beneficiaries =
            if (beneficiaries.isEmpty()) {
                emptyList()
            } else {
                walletEventBeneficiaryRepository
                    .saveAll(
                        beneficiaries.map { beneficiary ->
                            com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity(
                                walletEventId = walletEventSaved.id!!,
                                beneficiaryUserId = beneficiary.beneficiaryUserId,
                                benefitPercent = beneficiary.benefitPercent,
                            )
                        },
                    ).asFlow()
                    .toList()
                    .also { persisted ->
                        persisted.forEach { beneficiary ->
                            beneficiary.event = walletEventSaved
                        }
                    }
            }

        groupDebtService.applyWalletEvent(
            actorUserId = event.createdByUserId,
            event = walletEventSaved,
            entries = walletEventSaved.entries!!.filterIsInstance<WalletEntryEntity>(),
        )

        walletEventActionEventService.sendInsertedWalletEvent(event.createdByUserId, walletEventSaved)

        return walletEventSaved
    }

    private suspend fun resolveMaterializedTransferValues(
        entries: List<RecurrenceEntryEntity>,
        walletItemsById: Map<UUID, WalletItem>,
        date: LocalDate,
    ): Map<UUID, BigDecimal> {
        if (entries.size < 2 || entries.none { it.value < BigDecimal.ZERO }) {
            return emptyMap()
        }

        val originTemplate = entries.first { it.value < BigDecimal.ZERO }
        val targetTemplate = entries.first { it.walletItemId != originTemplate.walletItemId }
        val originItem = walletItemsById.getValue(originTemplate.walletItemId)
        val targetItem = walletItemsById.getValue(targetTemplate.walletItemId)
        val originValue = originTemplate.value.abs()
        val targetValue =
            if (originItem.currency == targetItem.currency) {
                originValue
            } else {
                exchangeRateService.convert(
                    value = originValue,
                    fromCurrency = originItem.currency,
                    toCurrency = targetItem.currency,
                    referenceDate = date,
                )
            }

        return mapOf(
            originTemplate.walletItemId to originValue.negate(),
            targetTemplate.walletItemId to targetValue,
        )
    }

    private fun calculateNextBillDate(
        billDate: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate {
        val candidate =
            this@WalletEntryCreateServiceImpl.recurrenceService.calculateNextDate(
                lastExecution = billDate,
                periodicity = periodicity,
            )

        return if (candidate.isSameMonthYear(billDate)) billDate else candidate.withDayOfMonth(1)
    }

    private suspend fun updateBalance(entry: WalletEntryEntity) {
        val event = entry.event!! as WalletEventEntity

        if (event.paymentType == PaymentType.INSTALLMENTS && entry.walletItem!!.type == WalletItemType.CREDIT_CARD) {
            if (event.installment == 1) {
                val installments = event.recurrenceEvent?.seriesQtyTotal ?: event.recurrenceEvent?.qtyLimit ?: 1

                walletItemService.addBalanceById(entry.walletItemId, entry.value.multiply(installments.toBigDecimal()))
            }
        } else {
            walletItemService.addBalanceById(entry.walletItemId, entry.value)
        }

        if (entry.billId != null) {
            creditCardBillService.addValueById(
                entry.billId,
                entry.value,
            )
        }
    }

    private suspend fun createWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEventEntity {
        val recurrenceConfig = createRecurrenceConfig(userId, newEntryRequest)
        val isInFuture = newEntryRequest.isInFuture(LocalDate.now(clock))

        return if (isInFuture) {
            recurrenceConfig!!
        } else {
            createNowWithoutCheckPermissions(
                userId = userId,
                newEntryRequest = newEntryRequest,
                recurrenceEvent = recurrenceConfig,
            )
        }.also {
            it.entries!!.forEachIndexed { index, entity ->
                val model =
                    when (newEntryRequest.type) {
                        WalletEntryType.TRANSFER ->
                            if (index == 0) {
                                newEntryRequest.origin
                            } else {
                                newEntryRequest.target
                            }
                        else -> newEntryRequest.resolvedSources!![index].walletItem
                    }
                entity.walletItem = model?.let { w -> walletItemMapper.fromModel(w) }
            }
        }
    }

    private suspend fun createNowWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        recurrenceEvent: RecurrenceEventEntity?,
    ): MinimumWalletEventEntity {
        val event =
            walletEventRepository
                .save(
                    requestToEventEntity(
                        id = null,
                        userId = userId,
                        newEntryRequest = newEntryRequest,
                        recurrenceConfig = recurrenceEvent,
                        installment = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) 1 else null,
                        date = newEntryRequest.date,
                    ),
                ).awaitSingle()

        event.recurrenceEvent = recurrenceEvent

        event.entries =
            walletEntryRepository
                .saveAll(
                    requestToEntryEntity(
                        id = null,
                        event = event,
                        newEntryRequest = newEntryRequest,
                    ),
                ).asFlow()
                .toList()
                .also {
                    it.forEach { entry ->
                        entry.event = event
                    }
                }

        return event.also {
            it.entries!!.filterIsInstance<WalletEntryEntity>().forEachIndexed { index, entry ->
                when (newEntryRequest.type) {
                    WalletEntryType.TRANSFER -> {
                        entry.bill = if (index == 0) newEntryRequest.originBill else newEntryRequest.targetBill
                        entry.walletItem =
                            walletItemMapper.fromModel(
                                if (index == 0) newEntryRequest.origin!! else newEntryRequest.target!!,
                            )
                    }
                    else -> {
                        val leg = newEntryRequest.resolvedSources!![index]
                        entry.bill = leg.bill
                        entry.walletItem = walletItemMapper.fromModel(leg.walletItem)
                    }
                }

                updateBalance(entry)
            }

            it.beneficiaries =
                walletEventBeneficiaryRepository
                    .saveAll(
                        requestToWalletEventBeneficiaryEntities(
                            eventId = requireNotNull(it.id),
                            newEntryRequest = newEntryRequest,
                        ),
                    ).asFlow()
                    .toList()
                    .also { persisted ->
                        persisted.forEach { beneficiary ->
                            beneficiary.event = it
                        }
                    }

            groupDebtService.applyWalletEvent(
                actorUserId = userId,
                event = it,
                entries = it.entries!!.filterIsInstance<WalletEntryEntity>(),
            )
        }
    }

    private suspend fun createRecurrenceConfig(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): RecurrenceEventEntity? =
        when (newEntryRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                requestToRecurrenceEntity(
                    id = null,
                    userId = userId,
                    newEntryRequest = newEntryRequest,
                    qtyLimit = newEntryRequest.installments!!,
                ).also { recurrenceEvent ->
                    persistRecurrenceBeneficiaries(recurrenceEvent, newEntryRequest)
                }

            PaymentType.RECURRING ->
                requestToRecurrenceEntity(
                    id = null,
                    userId = userId,
                    newEntryRequest = newEntryRequest,
                    qtyLimit = newEntryRequest.periodicityQtyLimit,
                ).also { recurrenceEvent ->
                    persistRecurrenceBeneficiaries(recurrenceEvent, newEntryRequest)
                }

            else -> {
                if (newEntryRequest.isInFuture(LocalDate.now(clock))) {
                    requestToRecurrenceEntity(
                        id = null,
                        userId = userId,
                        newEntryRequest = newEntryRequest,
                        qtyLimit = 1,
                    ).also { recurrenceEvent ->
                        persistRecurrenceBeneficiaries(recurrenceEvent, newEntryRequest)
                    }
                } else {
                    null
                }
            }
        }

    private suspend fun persistRecurrenceBeneficiaries(
        recurrenceEvent: RecurrenceEventEntity,
        newEntryRequest: NewEntryRequest,
    ) {
        recurrenceEvent.beneficiaries =
            recurrenceEventBeneficiaryRepository
                .saveAll(
                    requestToRecurrenceBeneficiaryEntities(
                        eventId = requireNotNull(recurrenceEvent.id),
                        newEntryRequest = newEntryRequest,
                    ),
                ).asFlow()
                .toList()
                .also { persisted ->
                    persisted.forEach { beneficiary ->
                        beneficiary.event = recurrenceEvent
                    }
                }
    }
}
