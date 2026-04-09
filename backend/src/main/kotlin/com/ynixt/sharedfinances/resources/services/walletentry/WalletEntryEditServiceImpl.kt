package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidRecurrenceQtyLimitException
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.walletentry.EditScheduledEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryEditService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEntryEditServiceImpl(
    private val walletEventRepository: WalletEventRepository,
    walletEntryRepository: WalletEntryRepository,
    private val walletEventActionEventService: WalletEventActionEventService,
    walletItemMapper: WalletItemMapper,
    groupService: GroupService,
    walletItemService: WalletItemService,
    creditCardBillService: CreditCardBillService,
    recurrenceService: RecurrenceService,
    recurrenceEventRepository: RecurrenceEventRepository,
    recurrenceSeriesRepository: RecurrenceSeriesRepository,
    recurrenceEntryRepository: RecurrenceEntryRepository,
    clock: Clock,
) : WalletEntryMutationSupportServiceImpl(
        walletEntryRepository = walletEntryRepository,
        walletItemMapper = walletItemMapper,
        groupService = groupService,
        walletItemService = walletItemService,
        creditCardBillService = creditCardBillService,
        recurrenceService = recurrenceService,
        recurrenceEventRepository = recurrenceEventRepository,
        recurrenceSeriesRepository = recurrenceSeriesRepository,
        recurrenceEntryRepository = recurrenceEntryRepository,
        clock = clock,
    ),
    WalletEntryEditService {
    @Transactional
    override suspend fun editOneOff(
        userId: UUID,
        walletEventId: UUID,
        request: NewEntryRequest,
    ): WalletEventEntity? {
        val existingEvent = walletEventRepository.findById(walletEventId).awaitSingleOrNull() ?: return null

        if (existingEvent.recurrenceEventId != null) {
            return null
        }

        val preparedRequest =
            normalizeAndAuthorize(
                userId = userId,
                request = request,
                existingGroupId = existingEvent.groupId,
                existingType = existingEvent.type,
            ) ?: return null

        val updated =
            editPostedEvent(
                userId = userId,
                existingEvent = existingEvent,
                preparedRequest = preparedRequest,
                recurrenceConfig = null,
            )

        walletEventActionEventService.sendUpdatedWalletEvent(userId, updated)
        return updated
    }

    @Transactional
    override suspend fun editScheduled(
        userId: UUID,
        recurrenceConfigId: UUID,
        request: EditScheduledEntryRequest,
    ): RecurrenceEventEntity? {
        val config = loadRecurrenceConfigWithEntries(recurrenceConfigId) ?: return null

        val preparedRequest =
            normalizeAndAuthorize(
                userId = userId,
                request = request.entry.copy(date = request.occurrenceDate),
                existingGroupId = config.groupId,
                existingType = config.type,
            ) ?: return null

        if (preparedRequest.paymentType != config.paymentType) {
            return null
        }

        if (request.scope == ScheduledEditScope.THIS_AND_FUTURE) {
            return handleThisAndFutureEdit(
                userId = userId,
                config = config,
                occurrenceDate = request.occurrenceDate,
                preparedRequest = preparedRequest,
            )
        }

        val generatedEvent =
            walletEventRepository
                .findOneByRecurrenceEventIdAndDate(
                    recurrenceEventId = recurrenceConfigId,
                    date = request.occurrenceDate,
                ).awaitSingleOrNull()

        return if (generatedEvent != null) {
            handleGeneratedOccurrenceEdit(
                userId = userId,
                config = config,
                generatedEvent = generatedEvent,
                preparedRequest = preparedRequest,
                scope = request.scope,
            )
        } else {
            handleNonGeneratedOccurrenceEdit(
                userId = userId,
                config = config,
                occurrenceDate = request.occurrenceDate,
                preparedRequest = preparedRequest,
                scope = request.scope,
            )
        }
    }

    private data class SeriesSelection(
        val segments: List<RecurrenceEventEntity>,
        val selectedSegment: RecurrenceEventEntity,
        val selectedPosition: ScheduledPosition?,
        val selectedGeneratedEvent: WalletEventEntity?,
        val selectedGlobalIndex: Int,
        val currentSeriesQtyTotal: Int?,
    )

    private data class GeneratedOccurrence(
        val segment: RecurrenceEventEntity,
        val event: WalletEventEntity,
        val globalIndex: Int,
    )

    private suspend fun handleThisAndFutureEdit(
        userId: UUID,
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
        preparedRequest: NewEntryRequest,
    ): RecurrenceEventEntity? {
        val selection = resolveSeriesSelection(config, occurrenceDate) ?: return null
        val segments = selection.segments
        val selectedSegment = selection.selectedSegment
        val selectedGlobalIndex = selection.selectedGlobalIndex
        val requestedSeriesQtyTotal = resolveRequestedSeriesQtyTotal(preparedRequest)
        val globalExecutedCount = segments.maxOf { it.seriesOffset + it.qtyExecuted }

        validateRequestedQtyLimit(
            requestedSeriesQtyTotal = requestedSeriesQtyTotal,
            alreadyExecuted = globalExecutedCount,
        )

        val targetSeriesQtyTotal = requestedSeriesQtyTotal ?: selection.currentSeriesQtyTotal
        syncSeriesQtyTotal(
            config = selectedSegment,
            targetSeriesQtyTotal = targetSeriesQtyTotal,
        )

        val oldTemplateRequest = prepareMutationRequest(userId, recurrenceToRequest(selectedSegment, occurrenceDate))
        val hasFutureSegmentsAfterBoundary = segments.any { it.id != selectedSegment.id && it.seriesOffset >= selectedGlobalIndex }
        val isEditingNextExecution = selectedSegment.nextExecution != null && occurrenceDate == selectedSegment.nextExecution

        if (
            isEditingNextExecution &&
            !hasFutureSegmentsAfterBoundary &&
            !hasTemplateChangesBeyondQtyLimit(oldTemplateRequest, preparedRequest)
        ) {
            val targetQtyLimit = targetSeriesQtyTotal?.let { (it - selectedSegment.seriesOffset).coerceAtLeast(0) }
            val targetNextExecution =
                if (targetQtyLimit != null && selectedSegment.qtyExecuted >= targetQtyLimit) {
                    null
                } else {
                    selectedSegment.nextExecution
                }
            val targetEndExecution =
                resolveUpdatedEndExecutionForInPlaceThisAndFuture(
                    config = selectedSegment,
                    qtyLimit = targetQtyLimit,
                    nextExecution = targetNextExecution,
                )

            val updated =
                persistRecurrenceCopy(
                    current = selectedSegment,
                    qtyLimit = targetQtyLimit,
                    nextExecution = targetNextExecution,
                    endExecution = targetEndExecution,
                )
            updated.seriesQtyTotal = selectedSegment.seriesQtyTotal

            walletEventActionEventService.sendUpdatedWalletEvent(userId, updated)
            return updated
        }

        val keptSelectedSegment =
            preserveSelectedSegmentBeforeBoundary(
                selectedSegment = selectedSegment,
                selectedPosition = selection.selectedPosition,
                selectedGeneratedEvent = selection.selectedGeneratedEvent,
                selectedGlobalIndex = selectedGlobalIndex,
            )

        val generatedOccurrences = loadGeneratedOccurrencesAtOrAfterIndex(segments, selectedGlobalIndex)
        val segmentsToDelete =
            segments.filter { segment ->
                val keepSelected = keptSelectedSegment?.id == segment.id
                !keepSelected && (segment.id == selectedSegment.id || segment.seriesOffset >= selectedGlobalIndex)
            }

        val remainingFromBoundary = targetSeriesQtyTotal?.let { (it - selectedGlobalIndex).coerceAtLeast(0) }
        val newUnifiedSegment =
            if (remainingFromBoundary != null && remainingFromBoundary == 0) {
                null
            } else {
                val unifiedRequest = buildRecurrenceRequest(preparedRequest, occurrenceDate, remainingFromBoundary)
                createRecurrenceConfig(
                    userId = userId,
                    preparedRequest = unifiedRequest,
                    qtyLimit = remainingFromBoundary,
                    seriesId = selectedSegment.seriesId,
                    seriesQtyTotal = selectedSegment.seriesQtyTotal,
                    seriesOffset = selectedGlobalIndex,
                )
            }

        val rewrittenCount =
            if (newUnifiedSegment != null) {
                rewriteGeneratedOccurrencesForUnifiedSegment(
                    userId = userId,
                    generatedOccurrences = generatedOccurrences,
                    preparedRequest = preparedRequest,
                    unifiedSegment = newUnifiedSegment,
                )
            } else {
                0
            }

        val unifiedWithExecutionState =
            if (newUnifiedSegment != null && rewrittenCount > 0) {
                val rewrittenDates = generatedOccurrences.map { it.event.date }
                val lastExecutionDate = rewrittenDates.maxOrNull()!!
                val nextExecutionDate =
                    recurrenceService.calculateNextExecution(
                        lastExecution = lastExecutionDate,
                        periodicity = newUnifiedSegment.periodicity,
                        qtyExecuted = rewrittenCount,
                        qtyLimit = newUnifiedSegment.qtyLimit,
                    )
                val endExecutionDate =
                    recurrenceService.calculateEndDate(
                        lastExecution = lastExecutionDate,
                        periodicity = newUnifiedSegment.periodicity,
                        qtyExecuted = rewrittenCount,
                        qtyLimit = newUnifiedSegment.qtyLimit,
                    )
                persistRecurrenceCopy(
                    current = newUnifiedSegment,
                    qtyLimit = newUnifiedSegment.qtyLimit,
                    qtyExecuted = rewrittenCount,
                    lastExecution = lastExecutionDate,
                    nextExecution = nextExecutionDate,
                    endExecution = endExecutionDate,
                ).also {
                    it.seriesQtyTotal = newUnifiedSegment.seriesQtyTotal
                }
            } else {
                newUnifiedSegment
            }

        segmentsToDelete
            .filter { segment -> unifiedWithExecutionState?.id != segment.id }
            .forEach { segment ->
                recurrenceEventRepository.deleteById(segment.id!!).awaitSingle()
            }

        val targetSegment = unifiedWithExecutionState ?: keptSelectedSegment ?: return null

        val affectedOccurrencesForInstallment =
            if (targetSeriesQtyTotal == null) {
                null
            } else {
                (targetSeriesQtyTotal - selectedGlobalIndex).coerceAtLeast(0)
            }
        val nonGeneratedAffectedOccurrences =
            if (affectedOccurrencesForInstallment == null) {
                0
            } else {
                (affectedOccurrencesForInstallment - rewrittenCount).coerceAtLeast(0)
            }

        applyInstallmentReservationDeltaBySegment(
            segments = segments,
            selectedGlobalIndex = selectedGlobalIndex,
            generatedOccurrences = generatedOccurrences,
            preparedRequest = preparedRequest,
            nonGeneratedFallbackCount = nonGeneratedAffectedOccurrences,
        )

        walletEventActionEventService.sendUpdatedWalletEvent(userId, targetSegment)
        return targetSegment
    }

    private suspend fun handleGeneratedOccurrenceEdit(
        userId: UUID,
        config: RecurrenceEventEntity,
        generatedEvent: WalletEventEntity,
        preparedRequest: NewEntryRequest,
        scope: ScheduledEditScope,
    ): RecurrenceEventEntity? {
        val recurrenceConfig = loadRecurrenceConfigWithEntries(config.id!!) ?: config
        val currentSeriesQtyTotal = resolveSeriesQtyTotal(recurrenceConfig)

        val updatedEvent =
            editPostedEvent(
                userId = userId,
                existingEvent = generatedEvent,
                preparedRequest = preparedRequest,
                recurrenceConfig = recurrenceConfig,
            )
        walletEventActionEventService.sendUpdatedWalletEvent(userId, updatedEvent)

        if (scope == ScheduledEditScope.ONLY_THIS) {
            return recurrenceConfig
        }

        if (scope != ScheduledEditScope.THIS_AND_FUTURE) {
            return null
        }

        val requestedSeriesQtyTotal = resolveRequestedSeriesQtyTotal(preparedRequest)
        val globalAlreadyExecuted = recurrenceConfig.seriesOffset + recurrenceConfig.qtyExecuted
        validateRequestedQtyLimit(
            requestedSeriesQtyTotal = requestedSeriesQtyTotal,
            alreadyExecuted = globalAlreadyExecuted,
        )

        val targetSeriesQtyTotal = requestedSeriesQtyTotal ?: currentSeriesQtyTotal
        syncSeriesQtyTotal(
            config = recurrenceConfig,
            targetSeriesQtyTotal = targetSeriesQtyTotal,
        )

        val nextDate = recurrenceConfig.nextExecution ?: return recurrenceConfig

        val remainingFutureCount = targetSeriesQtyTotal?.let { (it - globalAlreadyExecuted).coerceAtLeast(0) }

        val truncated =
            persistRecurrenceCopy(
                current = recurrenceConfig,
                qtyLimit = recurrenceConfig.qtyExecuted,
                nextExecution = null,
                endExecution = recurrenceConfig.lastExecution ?: generatedEvent.date,
            )
        truncated.seriesQtyTotal = recurrenceConfig.seriesQtyTotal

        if (remainingFutureCount != null && remainingFutureCount == 0) {
            return truncated
        }

        val successorRequest = buildRecurrenceRequest(preparedRequest, nextDate, remainingFutureCount)
        val successor =
            createRecurrenceConfig(
                userId = userId,
                preparedRequest = successorRequest,
                qtyLimit = remainingFutureCount,
                seriesId = recurrenceConfig.seriesId,
                seriesQtyTotal = recurrenceConfig.seriesQtyTotal,
                seriesOffset = recurrenceConfig.seriesOffset + recurrenceConfig.qtyExecuted,
            )

        applyInstallmentReservationDelta(
            config = recurrenceConfig,
            preparedRequest = preparedRequest,
            affectedOccurrences = remainingFutureCount ?: 1,
        )

        walletEventActionEventService.sendUpdatedWalletEvent(userId, successor)
        return successor
    }

    private suspend fun handleNonGeneratedOccurrenceEdit(
        userId: UUID,
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
        preparedRequest: NewEntryRequest,
        scope: ScheduledEditScope,
    ): RecurrenceEventEntity? {
        val position = resolveScheduledPosition(config, occurrenceDate) ?: return null
        val selectedSeriesOffset = calculateSeriesOffsetForPosition(config, position)
        val currentSeriesQtyTotal = resolveSeriesQtyTotal(config)

        val targetSeriesQtyTotal =
            if (scope == ScheduledEditScope.THIS_AND_FUTURE) {
                val requestedSeriesQtyTotal = resolveRequestedSeriesQtyTotal(preparedRequest)
                validateRequestedQtyLimit(
                    requestedSeriesQtyTotal = requestedSeriesQtyTotal,
                    alreadyExecuted = selectedSeriesOffset,
                )
                requestedSeriesQtyTotal ?: currentSeriesQtyTotal
            } else {
                currentSeriesQtyTotal
            }

        syncSeriesQtyTotal(
            config = config,
            targetSeriesQtyTotal = targetSeriesQtyTotal,
        )

        val oldTemplateRequest = prepareMutationRequest(userId, recurrenceToRequest(config, occurrenceDate))
        val isEditingNextExecution = config.nextExecution != null && occurrenceDate == config.nextExecution

        if (
            scope == ScheduledEditScope.THIS_AND_FUTURE &&
            isEditingNextExecution &&
            !hasTemplateChangesBeyondQtyLimit(oldTemplateRequest, preparedRequest)
        ) {
            val targetQtyLimit = targetSeriesQtyTotal?.let { (it - config.seriesOffset).coerceAtLeast(0) }
            val targetRemainingFromSelected = targetSeriesQtyTotal?.let { (it - selectedSeriesOffset).coerceAtLeast(0) }
            val targetNextExecution =
                if (targetRemainingFromSelected != null && targetRemainingFromSelected == 0) {
                    null
                } else {
                    config.nextExecution
                }
            val targetEndExecution =
                resolveUpdatedEndExecutionForInPlaceThisAndFuture(
                    config = config,
                    qtyLimit = targetQtyLimit,
                    nextExecution = targetNextExecution,
                )

            val updated =
                persistRecurrenceCopy(
                    current = config,
                    qtyLimit = targetQtyLimit,
                    nextExecution = targetNextExecution,
                    endExecution = targetEndExecution,
                )
            updated.seriesQtyTotal = config.seriesQtyTotal

            walletEventActionEventService.sendUpdatedWalletEvent(userId, updated)
            return updated
        }

        val truncated =
            persistRecurrenceTruncatedBeforePosition(
                config = config,
                position = position,
            )
        truncated.seriesQtyTotal = config.seriesQtyTotal

        val targetRemainingFromSelected =
            if (scope == ScheduledEditScope.THIS_AND_FUTURE) {
                targetSeriesQtyTotal?.let { (it - selectedSeriesOffset).coerceAtLeast(0) }
            } else {
                null
            }

        val selectedResult =
            when (scope) {
                ScheduledEditScope.ONLY_THIS -> {
                    val selectedRequest = buildRecurrenceRequest(preparedRequest, occurrenceDate, 1)
                    createRecurrenceConfig(
                        userId = userId,
                        preparedRequest = selectedRequest,
                        qtyLimit = 1,
                        seriesId = config.seriesId,
                        seriesQtyTotal = config.seriesQtyTotal,
                        seriesOffset = selectedSeriesOffset,
                    )
                }

                ScheduledEditScope.THIS_AND_FUTURE -> {
                    val remainingFromSelected = targetRemainingFromSelected
                    if (remainingFromSelected != null && remainingFromSelected == 0) {
                        null
                    } else {
                        val selectedRequest = buildRecurrenceRequest(preparedRequest, occurrenceDate, remainingFromSelected)
                        createRecurrenceConfig(
                            userId = userId,
                            preparedRequest = selectedRequest,
                            qtyLimit = remainingFromSelected,
                            seriesId = config.seriesId,
                            seriesQtyTotal = config.seriesQtyTotal,
                            seriesOffset = selectedSeriesOffset,
                        )
                    }
                }

                ScheduledEditScope.ALL_SERIES -> {
                    return null
                }
            }

        if (scope == ScheduledEditScope.ONLY_THIS) {
            val remainingAfterSelected = position.remainingAfterSelectedCount

            if (remainingAfterSelected == null || remainingAfterSelected > 0) {
                val nextDate = recurrenceService.calculateNextDate(occurrenceDate, config.periodicity)
                val successorRequest = buildRecurrenceRequest(oldTemplateRequest, nextDate, remainingAfterSelected)
                createRecurrenceConfig(
                    userId = userId,
                    preparedRequest = successorRequest,
                    qtyLimit = remainingAfterSelected,
                    seriesId = config.seriesId,
                    seriesQtyTotal = config.seriesQtyTotal,
                    seriesOffset = selectedSeriesOffset + 1,
                )
            }
        }

        val installmentOccurrences =
            when (scope) {
                ScheduledEditScope.ONLY_THIS -> 1
                ScheduledEditScope.THIS_AND_FUTURE -> targetRemainingFromSelected ?: position.remainingFromSelectedCount ?: 1
                ScheduledEditScope.ALL_SERIES -> 0
            }

        applyInstallmentReservationDelta(
            config = config,
            preparedRequest = preparedRequest,
            affectedOccurrences = installmentOccurrences,
        )

        walletEventActionEventService.sendUpdatedWalletEvent(userId, selectedResult ?: truncated)
        return if (scope == ScheduledEditScope.THIS_AND_FUTURE) selectedResult ?: truncated else truncated
    }

    private suspend fun editPostedEvent(
        userId: UUID,
        existingEvent: WalletEventEntity,
        preparedRequest: NewEntryRequest,
        recurrenceConfig: RecurrenceEventEntity?,
        recurrenceEventIdOverride: UUID? = null,
    ): WalletEventEntity {
        val oldEntries =
            walletEntryRepository
                .findAllByWalletEventId(existingEvent.id!!)
                .asFlow()
                .toList()

        rollbackPostedImpact(existingEvent, oldEntries, recurrenceConfig)

        val eventToPersist =
            WalletEventEntity(
                type = preparedRequest.type,
                name = preparedRequest.name,
                categoryId = preparedRequest.categoryId,
                userId = if (preparedRequest.groupId == null) userId else null,
                groupId = preparedRequest.groupId,
                tags = preparedRequest.tags?.ifEmpty { null },
                observations = preparedRequest.observations,
                date = preparedRequest.date,
                confirmed = preparedRequest.confirmed,
                installment = existingEvent.installment,
                recurrenceEventId = recurrenceEventIdOverride ?: existingEvent.recurrenceEventId,
                paymentType = preparedRequest.paymentType,
            ).also {
                it.id = existingEvent.id
                it.createdAt = existingEvent.createdAt
            }

        val savedEvent = walletEventRepository.save(eventToPersist).awaitSingle()

        walletEntryRepository.deleteAllByWalletEventId(savedEvent.id!!).awaitSingle()

        savedEvent.entries =
            walletEntryRepository
                .saveAll(
                    requestToEntryEntity(
                        id = null,
                        event = savedEvent,
                        newEntryRequest = preparedRequest,
                    ),
                ).asFlow()
                .toList()
                .also { entries ->
                    entries.forEachIndexed { index, entry ->
                        entry.event = savedEvent
                        entry.bill = if (index == 0) preparedRequest.originBill else preparedRequest.targetBill
                        entry.walletItem =
                            walletItemMapper.fromModel(
                                if (index == 0) preparedRequest.origin!! else preparedRequest.target!!,
                            )
                    }
                }

        applyPostedImpact(savedEvent, savedEvent.entries!!.filterIsInstance<WalletEntryEntity>(), recurrenceConfig)
        return savedEvent
    }

    private suspend fun resolveSeriesSelection(
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
    ): SeriesSelection? {
        val segmentRefs = recurrenceEventRepository.findAllBySeriesId(config.seriesId).asFlow().toList()
        val segments =
            buildList {
                if (segmentRefs.isEmpty()) {
                    add(config)
                } else {
                    segmentRefs
                        .sortedBy { it.seriesOffset }
                        .forEach { segment ->
                            val loaded = loadRecurrenceConfigWithEntries(segment.id!!)
                            if (loaded != null) {
                                add(loaded)
                            }
                        }
                }
            }

        val currentSeriesQtyTotal = resolveSeriesQtyTotal(segments.first())
        segments.forEach { segment ->
            segment.seriesQtyTotal = currentSeriesQtyTotal
        }

        val generatedBySegment =
            segments.associateWith { segment ->
                walletEventRepository
                    .findAllByRecurrenceEventId(segment.id!!)
                    .asFlow()
                    .toList()
                    .sortedBy { it.date }
            }

        segments.forEach { segment ->
            val generated = generatedBySegment[segment].orEmpty()
            val generatedIdx = generated.indexOfFirst { it.date == occurrenceDate }
            if (generatedIdx >= 0) {
                return SeriesSelection(
                    segments = segments,
                    selectedSegment = segment,
                    selectedPosition = null,
                    selectedGeneratedEvent = generated[generatedIdx],
                    selectedGlobalIndex = segment.seriesOffset + generatedIdx,
                    currentSeriesQtyTotal = currentSeriesQtyTotal,
                )
            }
        }

        segments.forEach { segment ->
            val position = resolveScheduledPosition(segment, occurrenceDate)
            if (position != null) {
                return SeriesSelection(
                    segments = segments,
                    selectedSegment = segment,
                    selectedPosition = position,
                    selectedGeneratedEvent = null,
                    selectedGlobalIndex = segment.seriesOffset + segment.qtyExecuted + position.selectedIndex,
                    currentSeriesQtyTotal = currentSeriesQtyTotal,
                )
            }
        }

        return null
    }

    private suspend fun preserveSelectedSegmentBeforeBoundary(
        selectedSegment: RecurrenceEventEntity,
        selectedPosition: ScheduledPosition?,
        selectedGeneratedEvent: WalletEventEntity?,
        selectedGlobalIndex: Int,
    ): RecurrenceEventEntity? {
        if (selectedGeneratedEvent == null) {
            val position = selectedPosition ?: return null
            if (position.beforeCount == 0 && selectedSegment.qtyExecuted == 0) {
                return null
            }
            return persistRecurrenceTruncatedBeforePosition(
                config = selectedSegment,
                position = position,
            ).also { it.seriesQtyTotal = selectedSegment.seriesQtyTotal }
        }

        val generatedInSegment =
            walletEventRepository
                .findAllByRecurrenceEventId(selectedSegment.id!!)
                .asFlow()
                .toList()
                .sortedBy { it.date }
        val localBoundaryIndex = (selectedGlobalIndex - selectedSegment.seriesOffset).coerceAtLeast(0)
        if (localBoundaryIndex == 0) {
            return null
        }

        val keepQtyExecuted = localBoundaryIndex
        val keepLastExecution = generatedInSegment.getOrNull(localBoundaryIndex - 1)?.date ?: selectedSegment.lastExecution
        val keepEndExecution = keepLastExecution

        return persistRecurrenceCopy(
            current = selectedSegment,
            qtyLimit = localBoundaryIndex,
            qtyExecuted = keepQtyExecuted,
            lastExecution = keepLastExecution,
            nextExecution = null,
            endExecution = keepEndExecution,
        ).also { it.seriesQtyTotal = selectedSegment.seriesQtyTotal }
    }

    private suspend fun loadGeneratedOccurrencesAtOrAfterIndex(
        segments: List<RecurrenceEventEntity>,
        selectedGlobalIndex: Int,
    ): List<GeneratedOccurrence> =
        segments
            .flatMap { segment ->
                walletEventRepository
                    .findAllByRecurrenceEventId(segment.id!!)
                    .asFlow()
                    .toList()
                    .sortedBy { it.date }
                    .mapIndexed { idx, event ->
                        GeneratedOccurrence(
                            segment = segment,
                            event = event,
                            globalIndex = segment.seriesOffset + idx,
                        )
                    }.filter { it.globalIndex >= selectedGlobalIndex }
            }.sortedBy { it.event.date }

    private suspend fun rewriteGeneratedOccurrencesForUnifiedSegment(
        userId: UUID,
        generatedOccurrences: List<GeneratedOccurrence>,
        preparedRequest: NewEntryRequest,
        unifiedSegment: RecurrenceEventEntity,
    ): Int {
        generatedOccurrences.forEach { occurrence ->
            val eventRequest =
                prepareMutationRequest(
                    userId = userId,
                    newEntryRequest = preparedRequest.copy(date = occurrence.event.date),
                )

            val updated =
                editPostedEvent(
                    userId = userId,
                    existingEvent = occurrence.event,
                    preparedRequest = eventRequest,
                    recurrenceConfig = unifiedSegment,
                    recurrenceEventIdOverride = unifiedSegment.id,
                )
            walletEventActionEventService.sendUpdatedWalletEvent(userId, updated)
        }

        return generatedOccurrences.size
    }

    private suspend fun applyInstallmentReservationDeltaBySegment(
        segments: List<RecurrenceEventEntity>,
        selectedGlobalIndex: Int,
        generatedOccurrences: List<GeneratedOccurrence>,
        preparedRequest: NewEntryRequest,
        nonGeneratedFallbackCount: Int,
    ) {
        if (segments.isEmpty()) {
            return
        }

        if (segments.first().paymentType != PaymentType.INSTALLMENTS || preparedRequest.origin?.type != WalletItemType.CREDIT_CARD) {
            return
        }

        val newValue = extractSignedEntryValue(preparedRequest)
        val generatedCountBySegment = generatedOccurrences.groupBy { it.segment.id!! }.mapValues { (_, value) -> value.size }

        var totalDelta = BigDecimal.ZERO
        var totalNonGeneratedCount = 0

        segments.forEach { segment ->
            val segmentLimit = segment.qtyLimit ?: return@forEach
            val segmentGenerated = generatedCountBySegment[segment.id!!] ?: 0
            val localBoundary = (selectedGlobalIndex - segment.seriesOffset).coerceAtLeast(0)
            val localStart = maxOf(localBoundary, segmentGenerated, segment.qtyExecuted)
            val affectedNonGenerated = (segmentLimit - localStart).coerceAtLeast(0)
            if (affectedNonGenerated <= 0) {
                return@forEach
            }

            val oldValue =
                segment.entries!!
                    .filterIsInstance<RecurrenceEntryEntity>()
                    .first()
                    .value
            totalDelta = totalDelta.add(newValue.subtract(oldValue).multiply(affectedNonGenerated.toBigDecimal()))
            totalNonGeneratedCount += affectedNonGenerated
        }

        if (totalNonGeneratedCount == 0 && nonGeneratedFallbackCount > 0) {
            val oldValue =
                segments
                    .first()
                    .entries!!
                    .filterIsInstance<RecurrenceEntryEntity>()
                    .first()
                    .value
            totalDelta = newValue.subtract(oldValue).multiply(nonGeneratedFallbackCount.toBigDecimal())
        }

        if (totalDelta != BigDecimal.ZERO) {
            walletItemService.addBalanceById(preparedRequest.originId, totalDelta)
        }
    }

    private fun extractSignedEntryValue(preparedRequest: NewEntryRequest): BigDecimal =
        when (preparedRequest.type) {
            WalletEntryType.REVENUE -> preparedRequest.value
            WalletEntryType.EXPENSE -> preparedRequest.value.negate()
            WalletEntryType.TRANSFER -> preparedRequest.value.negate()
        }

    private suspend fun normalizeAndAuthorize(
        userId: UUID,
        request: NewEntryRequest,
        existingGroupId: UUID?,
        existingType: WalletEntryType,
    ): NewEntryRequest? {
        val requestGroupId = request.groupId
        if (requestGroupId != existingGroupId) {
            return null
        }

        if (isTransferBoundaryViolation(existingType = existingType, requestType = request.type)) {
            return null
        }

        val requestWithOwnership = request.copy(groupId = existingGroupId)
        val preparedRequest = prepareMutationRequest(userId, requestWithOwnership)

        return if (hasMutationPermission(userId, preparedRequest)) preparedRequest else null
    }

    private fun isTransferBoundaryViolation(
        existingType: WalletEntryType,
        requestType: WalletEntryType,
    ): Boolean {
        val existingIsTransfer = existingType == WalletEntryType.TRANSFER
        val requestIsTransfer = requestType == WalletEntryType.TRANSFER
        return existingIsTransfer != requestIsTransfer
    }

    private suspend fun applyInstallmentReservationDelta(
        config: RecurrenceEventEntity,
        preparedRequest: NewEntryRequest,
        affectedOccurrences: Int,
    ) {
        if (config.paymentType != PaymentType.INSTALLMENTS || affectedOccurrences <= 0) {
            return
        }

        if (preparedRequest.origin?.type != WalletItemType.CREDIT_CARD) {
            return
        }

        val oldValue =
            config.entries!!
                .filterIsInstance<RecurrenceEntryEntity>()
                .first()
                .value
        val newValue =
            when (preparedRequest.type) {
                WalletEntryType.REVENUE -> preparedRequest.value
                WalletEntryType.EXPENSE -> preparedRequest.value.negate()
                WalletEntryType.TRANSFER -> preparedRequest.value.negate()
            }

        val delta = newValue.subtract(oldValue).multiply(affectedOccurrences.toBigDecimal())
        if (delta != BigDecimal.ZERO) {
            walletItemService.addBalanceById(preparedRequest.originId, delta)
        }
    }

    private fun resolveRequestedSeriesQtyTotal(request: NewEntryRequest): Int? =
        when (request.paymentType) {
            PaymentType.INSTALLMENTS -> request.installments
            PaymentType.RECURRING -> request.periodicityQtyLimit
            else -> null
        }

    private fun hasTemplateChangesBeyondQtyLimit(
        currentRequest: NewEntryRequest,
        preparedRequest: NewEntryRequest,
    ): Boolean =
        currentRequest.type != preparedRequest.type ||
            currentRequest.groupId != preparedRequest.groupId ||
            currentRequest.originId != preparedRequest.originId ||
            currentRequest.targetId != preparedRequest.targetId ||
            currentRequest.name != preparedRequest.name ||
            currentRequest.categoryId != preparedRequest.categoryId ||
            currentRequest.value.compareTo(preparedRequest.value) != 0 ||
            currentRequest.observations != preparedRequest.observations ||
            currentRequest.paymentType != preparedRequest.paymentType ||
            currentRequest.periodicity != preparedRequest.periodicity ||
            currentRequest.originBillDate != preparedRequest.originBillDate ||
            currentRequest.targetBillDate != preparedRequest.targetBillDate ||
            normalizeTags(currentRequest.tags) != normalizeTags(preparedRequest.tags)

    private fun normalizeTags(tags: List<String>?): List<String>? = tags?.ifEmpty { null }

    private fun validateRequestedQtyLimit(
        requestedSeriesQtyTotal: Int?,
        alreadyExecuted: Int,
    ) {
        if (requestedSeriesQtyTotal != null && requestedSeriesQtyTotal < alreadyExecuted) {
            throw InvalidRecurrenceQtyLimitException(
                requestedQtyLimit = requestedSeriesQtyTotal,
                alreadyExecuted = alreadyExecuted,
            )
        }
    }

    private suspend fun syncSeriesQtyTotal(
        config: RecurrenceEventEntity,
        targetSeriesQtyTotal: Int?,
    ) {
        if (config.seriesQtyTotal == targetSeriesQtyTotal) {
            return
        }

        recurrenceSeriesRepository.updateQtyTotal(config.seriesId, targetSeriesQtyTotal).awaitSingle()
        config.seriesQtyTotal = targetSeriesQtyTotal
    }

    private fun resolveUpdatedEndExecutionForInPlaceThisAndFuture(
        config: RecurrenceEventEntity,
        qtyLimit: Int?,
        nextExecution: LocalDate?,
    ): LocalDate? {
        if (nextExecution == null) {
            return config.lastExecution
        }

        return recurrenceService.calculateEndDate(
            lastExecution = config.lastExecution ?: nextExecution,
            periodicity = config.periodicity,
            qtyExecuted = config.qtyExecuted,
            qtyLimit = qtyLimit?.let { if (config.lastExecution == null) it - 1 else it },
        )
    }
}
