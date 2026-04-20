package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletSourceLeg
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

abstract class WalletEntryMutationSupportServiceImpl(
    protected val walletEntryRepository: WalletEntryRepository,
    protected val walletItemMapper: WalletItemMapper,
    protected val groupDebtService: GroupDebtService,
    groupService: GroupService,
    walletItemService: WalletItemService,
    genericCategoryService: GenericCategoryService,
    categoryConceptService: CategoryConceptService,
    creditCardBillService: CreditCardBillService,
    recurrenceService: RecurrenceService,
    recurrenceEventRepository: RecurrenceEventRepository,
    recurrenceSeriesRepository: RecurrenceSeriesRepository,
    recurrenceEntryRepository: RecurrenceEntryRepository,
    walletEventBeneficiaryRepository: WalletEventBeneficiarySpringDataRepository,
    recurrenceEventBeneficiaryRepository: RecurrenceEventBeneficiarySpringDataRepository,
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
    ) {
    protected data class ScheduledPosition(
        val selectedIndex: Int,
        val beforeCount: Int,
        val remainingFromSelectedCount: Int?,
        val remainingAfterSelectedCount: Int?,
    )

    private val maxScheduleIterations = 2048

    protected suspend fun hydratePostedEventForMutation(event: WalletEventEntity): WalletEventEntity {
        val entries =
            walletEntryRepository
                .findAllByWalletEventId(event.id!!)
                .asFlow()
                .toList()

        event.entries = attachEntriesWithWalletItems(event, entries)
        event.beneficiaries =
            walletEventBeneficiaryRepository
                .findAllByWalletEventId(event.id!!)
                .asFlow()
                .toList()
                .also { loaded ->
                    loaded.forEach { beneficiary ->
                        beneficiary.event = event
                    }
                }

        return event
    }

    protected suspend fun rollbackPostedImpact(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
        recurrenceConfig: RecurrenceEventEntity?,
    ) {
        entries.forEach { entry ->
            val walletDelta =
                getWalletImpactForEntry(
                    event = event,
                    entry = entry,
                    recurrenceConfig = recurrenceConfig,
                )

            if (walletDelta != BigDecimal.ZERO) {
                walletItemService.addBalanceById(entry.walletItemId, walletDelta.negate())
            }

            if (entry.billId != null) {
                creditCardBillService.addValueById(entry.billId, entry.value.negate())
            }
        }

        groupDebtService.rollbackWalletEvent(actorUserId = actorUserId, event = event)
    }

    protected suspend fun applyPostedImpact(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
        recurrenceConfig: RecurrenceEventEntity?,
    ) {
        entries.forEach { entry ->
            val walletDelta =
                getWalletImpactForEntry(
                    event = event,
                    entry = entry,
                    recurrenceConfig = recurrenceConfig,
                )

            if (walletDelta != BigDecimal.ZERO) {
                walletItemService.addBalanceById(entry.walletItemId, walletDelta)
            }

            if (entry.billId != null) {
                creditCardBillService.addValueById(entry.billId, entry.value)
            }
        }

        groupDebtService.applyWalletEvent(
            actorUserId = actorUserId,
            event = event,
            entries = entries,
        )
    }

    protected fun getWalletImpactForEntry(
        event: WalletEventEntity,
        entry: WalletEntryEntity,
        recurrenceConfig: RecurrenceEventEntity?,
    ): BigDecimal {
        val walletItemType = entry.walletItem?.type

        if (event.paymentType != PaymentType.INSTALLMENTS || walletItemType != WalletItemType.CREDIT_CARD) {
            return entry.value
        }

        if (event.recurrenceEventId == null) {
            return entry.value
        }

        if (event.installment != 1) {
            return BigDecimal.ZERO
        }

        val installments =
            recurrenceConfig?.seriesQtyTotal
                ?: event.recurrenceEvent?.seriesQtyTotal
                ?: recurrenceConfig?.qtyLimit
                ?: event.recurrenceEvent?.qtyLimit
                ?: 1

        return entry.value.multiply(installments.toBigDecimal())
    }

    protected suspend fun loadRecurrenceConfigWithEntries(id: UUID): RecurrenceEventEntity? {
        val config = recurrenceEventRepository.findById(id).awaitSingleOrNull() ?: return null
        config.seriesQtyTotal = recurrenceSeriesRepository.findById(config.seriesId).awaitSingleOrNull()?.qtyTotal
        val entries = recurrenceEntryRepository.findAllByWalletEventId(id).asFlow().toList()

        config.entries = attachEntriesWithWalletItems(config, entries)
        config.beneficiaries =
            recurrenceEventBeneficiaryRepository
                .findAllByWalletEventId(id)
                .asFlow()
                .toList()
                .also { loaded ->
                    loaded.forEach { beneficiary ->
                        beneficiary.event = config
                    }
                }
        return config
    }

    private suspend fun <TEntry : MinimumWalletEntryEntity> attachEntriesWithWalletItems(
        event: MinimumWalletEventEntity,
        entries: List<TEntry>,
    ): List<TEntry> {
        val walletItemsById =
            walletItemService
                .findAllByIdIn(entries.map { it.walletItemId }.toSet())
                .toList()
                .associateBy { it.id!! }

        return entries.also { list ->
            list.forEach { entry ->
                entry.event = event
                val walletItem = walletItemsById[entry.walletItemId]
                if (walletItem != null) {
                    entry.walletItem = walletItemMapper.fromModel(walletItem)
                }
            }
        }
    }

    protected fun recurrenceToRequest(
        config: RecurrenceEventEntity,
        date: LocalDate,
    ): NewEntryRequest {
        val entries = config.entries!!.filterIsInstance<RecurrenceEntryEntity>()

        if (config.type == WalletEntryType.TRANSFER) {
            val origin = entries.first { it.value < BigDecimal.ZERO }
            val target = entries.first { it.walletItemId != origin.walletItemId }

            return NewEntryRequest(
                type = config.type,
                groupId = config.groupId,
                originId = origin.walletItemId,
                targetId = target.walletItemId,
                name = config.name,
                categoryId = config.categoryId,
                date = date,
                value = null,
                originValue = origin.value.abs(),
                targetValue = null,
                confirmed = false,
                observations = config.observations,
                paymentType = config.paymentType,
                installments = if (config.paymentType == PaymentType.INSTALLMENTS) config.qtyLimit else null,
                periodicity = config.periodicity,
                periodicityQtyLimit = if (config.paymentType == PaymentType.RECURRING) config.qtyLimit else null,
                originBillDate = origin.nextBillDate,
                targetBillDate = target.nextBillDate,
                tags = config.tags,
                transferPurpose = config.transferPurpose,
                beneficiaries =
                    config.beneficiaries?.map { beneficiary ->
                        NewWalletBeneficiaryLeg(
                            userId = beneficiary.beneficiaryUserId,
                            benefitPercent = beneficiary.benefitPercent,
                        )
                    },
            )
        }

        val sources =
            entries.map { e ->
                NewWalletSourceLeg(
                    walletItemId = e.walletItemId,
                    contributionPercent = e.contributionPercent ?: BigDecimal("100.00"),
                    billDate = e.nextBillDate,
                )
            }
        val value =
            when (config.type) {
                WalletEntryType.REVENUE -> entries.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.value) }
                WalletEntryType.EXPENSE -> entries.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.value) }.negate()
                WalletEntryType.TRANSFER -> null
            }

        return NewEntryRequest(
            type = config.type,
            groupId = config.groupId,
            originId = entries.first().walletItemId,
            targetId = null,
            name = config.name,
            categoryId = config.categoryId,
            date = date,
            value = value,
            originValue = null,
            targetValue = null,
            confirmed = false,
            observations = config.observations,
            paymentType = config.paymentType,
            installments = if (config.paymentType == PaymentType.INSTALLMENTS) config.qtyLimit else null,
            periodicity = config.periodicity,
            periodicityQtyLimit = if (config.paymentType == PaymentType.RECURRING) config.qtyLimit else null,
            originBillDate = entries.first().nextBillDate,
            targetBillDate = null,
            tags = config.tags,
            transferPurpose = config.transferPurpose,
            sources = sources,
            beneficiaries =
                config.beneficiaries?.map { beneficiary ->
                    NewWalletBeneficiaryLeg(
                        userId = beneficiary.beneficiaryUserId,
                        benefitPercent = beneficiary.benefitPercent,
                    )
                },
        )
    }

    protected fun resolveScheduledPosition(
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
    ): ScheduledPosition? {
        val nextExecution = config.nextExecution ?: return null
        val totalFutureCount = config.qtyLimit?.let { (it - config.qtyExecuted).coerceAtLeast(0) }
        var cursorDate = nextExecution
        var selectedIndex: Int? = null
        var visited = 0

        while (visited < maxScheduleIterations) {
            if (cursorDate == occurrenceDate) {
                selectedIndex = visited
                break
            }

            val totalBounded = totalFutureCount
            if (totalBounded != null && visited >= totalBounded - 1) {
                break
            }

            val nextDate = recurrenceService.calculateNextDate(cursorDate, config.periodicity)
            if (!nextDate.isAfter(cursorDate)) {
                break
            }

            cursorDate = nextDate
            visited++
        }

        val idx = selectedIndex ?: return null
        val beforeCount = idx
        val remainingFromSelected =
            totalFutureCount?.let {
                (it - beforeCount).coerceAtLeast(0)
            }
        val remainingAfterSelected =
            totalFutureCount?.let {
                (it - beforeCount - 1).coerceAtLeast(0)
            }

        return ScheduledPosition(
            selectedIndex = idx,
            beforeCount = beforeCount,
            remainingFromSelectedCount = remainingFromSelected,
            remainingAfterSelectedCount = remainingAfterSelected,
        )
    }

    protected fun calculateDateAtIndex(
        startDate: LocalDate,
        periodicity: RecurrenceType,
        index: Int,
    ): LocalDate {
        var date = startDate
        repeat(index) {
            date = recurrenceService.calculateNextDate(date, periodicity)
        }
        return date
    }

    protected suspend fun persistRecurrenceCopy(
        current: RecurrenceEventEntity,
        qtyLimit: Int?,
        qtyExecuted: Int = current.qtyExecuted,
        lastExecution: LocalDate? = current.lastExecution,
        nextExecution: LocalDate?,
        endExecution: LocalDate?,
    ): RecurrenceEventEntity =
        recurrenceEventRepository
            .save(
                RecurrenceEventEntity(
                    name = current.name,
                    categoryId = current.categoryId,
                    createdByUserId = current.createdByUserId,
                    groupId = current.groupId,
                    tags = current.tags,
                    observations = current.observations,
                    type = current.type,
                    periodicity = current.periodicity,
                    paymentType = current.paymentType,
                    qtyExecuted = qtyExecuted,
                    qtyLimit = qtyLimit,
                    lastExecution = lastExecution,
                    nextExecution = nextExecution,
                    endExecution = endExecution,
                    seriesId = current.seriesId,
                    seriesOffset = current.seriesOffset,
                    transferPurpose = current.transferPurpose,
                ).also {
                    it.id = current.id
                    it.createdAt = current.createdAt
                    it.entries = current.entries
                    it.beneficiaries = current.beneficiaries
                    it.seriesQtyTotal = current.seriesQtyTotal
                },
            ).awaitSingle()

    protected fun buildRecurrenceRequest(
        preparedRequest: NewEntryRequest,
        date: LocalDate,
        qtyLimit: Int?,
    ): NewEntryRequest =
        when (preparedRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                preparedRequest.copy(
                    date = date,
                    installments = qtyLimit,
                    periodicity = preparedRequest.periodicity ?: RecurrenceType.MONTHLY,
                )

            PaymentType.RECURRING ->
                preparedRequest.copy(
                    date = date,
                    periodicityQtyLimit = qtyLimit,
                    periodicity = preparedRequest.periodicity ?: RecurrenceType.MONTHLY,
                )

            else ->
                preparedRequest.copy(
                    date = date,
                    periodicity = RecurrenceType.SINGLE,
                )
        }

    protected fun resolveSeriesQtyTotal(config: RecurrenceEventEntity): Int? = config.seriesQtyTotal ?: config.qtyLimit

    protected fun calculateSeriesOffsetForPosition(
        config: RecurrenceEventEntity,
        position: ScheduledPosition,
    ): Int = config.seriesOffset + config.qtyExecuted + position.selectedIndex

    protected suspend fun createRecurrenceConfig(
        userId: UUID,
        preparedRequest: NewEntryRequest,
        qtyLimit: Int?,
        seriesId: UUID? = null,
        seriesQtyTotal: Int? = null,
        seriesOffset: Int = 0,
    ): RecurrenceEventEntity =
        requestToRecurrenceEntity(
            id = null,
            userId = userId,
            newEntryRequest = preparedRequest,
            qtyLimit = qtyLimit,
            seriesId = seriesId,
            seriesQtyTotal = seriesQtyTotal,
            seriesOffset = seriesOffset,
        ).also { recurrence ->
            val origin = preparedRequest.origin
            val target = preparedRequest.target
            val sourcesById =
                preparedRequest.resolvedSources?.associateBy { it.walletItemId }.orEmpty()

            recurrence.entries?.forEach { entry ->
                val model =
                    when (preparedRequest.type) {
                        WalletEntryType.TRANSFER ->
                            when (entry.walletItemId) {
                                preparedRequest.originId -> origin
                                preparedRequest.targetId -> target
                                else -> null
                            }
                        else -> sourcesById[entry.walletItemId]?.walletItem
                    } ?: return@forEach

                entry.walletItem = walletItemMapper.fromModel(model)
            }

            recurrence.beneficiaries =
                recurrenceEventBeneficiaryRepository
                    .saveAll(
                        requestToRecurrenceBeneficiaryEntities(
                            eventId = requireNotNull(recurrence.id),
                            newEntryRequest = preparedRequest,
                        ),
                    ).asFlow()
                    .toList()
                    .also { persisted ->
                        persisted.forEach { beneficiary ->
                            beneficiary.event = recurrence
                        }
                    }
        }

    protected suspend fun persistRecurrenceTruncatedBeforePosition(
        config: RecurrenceEventEntity,
        position: ScheduledPosition,
    ): RecurrenceEventEntity =
        if (position.beforeCount == 0) {
            persistRecurrenceCopy(
                current = config,
                qtyLimit = config.qtyExecuted,
                nextExecution = null,
                endExecution = config.lastExecution,
            )
        } else {
            val beforeLastDate = calculateDateAtIndex(config.nextExecution!!, config.periodicity, position.beforeCount - 1)
            persistRecurrenceCopy(
                current = config,
                qtyLimit = config.qtyExecuted + position.beforeCount,
                nextExecution = config.nextExecution,
                endExecution = beforeLastDate,
            )
        }
}
