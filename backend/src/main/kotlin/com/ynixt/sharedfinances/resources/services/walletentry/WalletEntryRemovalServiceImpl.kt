package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEntryRemovalServiceImpl(
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
    WalletEntryRemovalService {
    @Transactional
    override suspend fun deleteOneOff(
        userId: UUID,
        walletEventId: UUID,
    ): WalletEventEntity? {
        val existingEvent = walletEventRepository.findById(walletEventId).awaitSingleOrNull() ?: return null

        if (existingEvent.recurrenceEventId != null) {
            return null
        }

        if (!hasDeletePermission(userId = userId, ownerUserId = existingEvent.userId, groupId = existingEvent.groupId)) {
            return null
        }

        val payloadEvent = hydratePostedEventForMutation(existingEvent)

        deletePostedEvents(
            events = listOf(payloadEvent),
            recurrenceConfig = null,
        )

        walletEventActionEventService.sendDeletedWalletEvent(userId, payloadEvent)
        return payloadEvent
    }

    @Transactional
    override suspend fun deleteScheduled(
        userId: UUID,
        recurrenceConfigId: UUID,
        request: DeleteScheduledEntryRequest,
    ): MinimumWalletEventEntity? {
        val config = loadRecurrenceConfigWithEntries(recurrenceConfigId) ?: return null

        if (!hasDeletePermission(userId = userId, ownerUserId = config.userId, groupId = config.groupId)) {
            return null
        }

        val scope = normalizeDeleteScope(config, request.scope) ?: return null
        val generatedEvent =
            walletEventRepository
                .findOneByRecurrenceEventIdAndDate(
                    recurrenceEventId = recurrenceConfigId,
                    date = request.occurrenceDate,
                ).awaitSingleOrNull()
        val futurePosition = resolveScheduledPosition(config, request.occurrenceDate)

        if (generatedEvent == null && futurePosition == null) {
            return null
        }

        val payloadEvent =
            generatedEvent?.let { hydratePostedEventForMutation(it) }
                ?: buildScheduledDeletePayload(
                    config = config,
                    occurrenceDate = request.occurrenceDate,
                    futurePosition = futurePosition,
                )

        when (scope) {
            ScheduledEditScope.ONLY_THIS -> {
                if (generatedEvent != null) {
                    deletePostedEvents(events = listOf(generatedEvent), recurrenceConfig = config)
                } else {
                    handleNonGeneratedOccurrenceDeleteOnlyThis(
                        userId = userId,
                        config = config,
                        occurrenceDate = request.occurrenceDate,
                        position = requireNotNull(futurePosition),
                    )
                }
            }

            ScheduledEditScope.THIS_AND_FUTURE -> {
                if (generatedEvent != null) {
                    handleGeneratedOccurrenceDeleteThisAndFuture(
                        config = config,
                        occurrenceDate = request.occurrenceDate,
                    )
                } else {
                    persistRecurrenceTruncatedBeforePosition(
                        config = config,
                        position = requireNotNull(futurePosition),
                    )
                }
            }

            ScheduledEditScope.ALL_SERIES -> {
                handleDeleteAllSeries(config)
            }
        }

        walletEventActionEventService.sendDeletedWalletEvent(userId, payloadEvent)
        return payloadEvent
    }

    private suspend fun handleNonGeneratedOccurrenceDeleteOnlyThis(
        userId: UUID,
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
        position: ScheduledPosition,
    ) {
        val selectedSeriesOffset = calculateSeriesOffsetForPosition(config, position)
        val seriesQtyTotal = resolveSeriesQtyTotal(config)

        persistRecurrenceTruncatedBeforePosition(
            config = config,
            position = position,
        )

        val remainingAfterSelected = position.remainingAfterSelectedCount
        if (remainingAfterSelected == null || remainingAfterSelected > 0) {
            val nextDate = recurrenceService.calculateNextDate(occurrenceDate, config.periodicity)
            val oldTemplateRequest = prepareMutationRequest(userId, recurrenceToRequest(config, occurrenceDate))
            val successorRequest = buildRecurrenceRequest(oldTemplateRequest, nextDate, remainingAfterSelected)
            createRecurrenceConfig(
                userId = userId,
                preparedRequest = successorRequest,
                qtyLimit = remainingAfterSelected,
                seriesId = config.seriesId,
                seriesQtyTotal = seriesQtyTotal,
                seriesOffset = selectedSeriesOffset + 1,
            )
        }
    }

    private suspend fun handleGeneratedOccurrenceDeleteThisAndFuture(
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
    ) {
        val generatedEvents =
            walletEventRepository
                .findAllByRecurrenceEventId(config.id!!)
                .asFlow()
                .toList()

        val eventsToDelete = generatedEvents.filter { !it.date.isBefore(occurrenceDate) }
        if (eventsToDelete.isNotEmpty()) {
            deletePostedEvents(
                events = eventsToDelete,
                recurrenceConfig = config,
            )
        }

        val keptEvents = generatedEvents.filter { it.date.isBefore(occurrenceDate) }
        val keptLastDate = keptEvents.maxByOrNull { it.date }?.date
        val keptCount = keptEvents.size

        persistRecurrenceCopy(
            current = config,
            qtyLimit = keptCount,
            qtyExecuted = keptCount,
            lastExecution = keptLastDate,
            nextExecution = null,
            endExecution = keptLastDate,
        )
    }

    private suspend fun handleDeleteAllSeries(config: RecurrenceEventEntity) {
        val series = recurrenceSeriesRepository.findById(config.seriesId).awaitSingleOrNull()
        val configsInSeries =
            recurrenceEventRepository
                .findAllBySeriesId(config.seriesId)
                .asFlow()
                .toList()
                .ifEmpty { listOf(config) }

        configsInSeries.forEach { segment ->
            segment.seriesQtyTotal = series?.qtyTotal
            val generatedEvents =
                walletEventRepository
                    .findAllByRecurrenceEventId(segment.id!!)
                    .asFlow()
                    .toList()

            if (generatedEvents.isNotEmpty()) {
                deletePostedEvents(
                    events = generatedEvents,
                    recurrenceConfig = segment,
                    deleteFromRepository = false,
                )
            }
        }

        recurrenceSeriesRepository.deleteById(config.seriesId).awaitSingle()
    }

    private suspend fun deletePostedEvents(
        events: List<WalletEventEntity>,
        recurrenceConfig: RecurrenceEventEntity?,
        deleteFromRepository: Boolean = true,
    ) {
        events.forEach { event ->
            val hydratedEvent = hydratePostedEventForMutation(event)
            rollbackPostedImpact(
                event = hydratedEvent,
                entries = hydratedEvent.entries!!.filterIsInstance<WalletEntryEntity>(),
                recurrenceConfig = recurrenceConfig,
            )
            if (deleteFromRepository) {
                walletEntryRepository.deleteAllByWalletEventId(hydratedEvent.id!!).awaitSingle()
                walletEventRepository.deleteById(hydratedEvent.id!!).awaitSingle()
            }
        }
    }

    private fun normalizeDeleteScope(
        config: RecurrenceEventEntity,
        scope: ScheduledEditScope?,
    ): ScheduledEditScope? {
        if (config.paymentType == PaymentType.UNIQUE) {
            return ScheduledEditScope.ONLY_THIS
        }

        return scope
    }

    private fun buildScheduledDeletePayload(
        config: RecurrenceEventEntity,
        occurrenceDate: LocalDate,
        futurePosition: ScheduledPosition?,
    ): RecurrenceEventEntity =
        RecurrenceEventEntity(
            name = config.name,
            categoryId = config.categoryId,
            userId = config.userId,
            groupId = config.groupId,
            tags = config.tags,
            observations = config.observations,
            type = config.type,
            periodicity = config.periodicity,
            paymentType = config.paymentType,
            qtyExecuted = config.qtyExecuted + (futurePosition?.selectedIndex ?: 0),
            qtyLimit = config.qtyLimit,
            lastExecution = config.lastExecution,
            nextExecution = occurrenceDate,
            endExecution = config.endExecution,
            seriesId = config.seriesId,
            seriesOffset = config.seriesOffset,
        ).also {
            it.id = config.id
            it.createdAt = config.createdAt
            it.updatedAt = config.updatedAt
            it.entries = config.entries
            it.seriesQtyTotal = config.seriesQtyTotal
        }

    private suspend fun hasDeletePermission(
        userId: UUID,
        ownerUserId: UUID?,
        groupId: UUID?,
    ): Boolean {
        if (groupId == null) {
            return ownerUserId == userId
        }

        val group = groupService.findGroupWithAssociatedItems(userId, groupId) ?: return false
        return group.permissions.contains(GroupPermissions.SEND_ENTRIES)
    }
}
