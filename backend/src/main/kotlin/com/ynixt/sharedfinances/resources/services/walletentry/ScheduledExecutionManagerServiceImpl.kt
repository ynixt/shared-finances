package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.ScheduledExecutionManagerRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.ScheduledExecutionManagerService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class ScheduledExecutionManagerServiceImpl(
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val walletEventRepository: WalletEventRepository,
    private val walletEventListService: WalletEventListService,
    private val groupPermissionService: GroupPermissionService,
    private val clock: Clock,
) : ScheduledExecutionManagerService {
    private val maxGeneratedItems = 1000

    override suspend fun list(
        userId: UUID,
        request: ScheduledExecutionManagerRequest,
    ): List<EventListResponse> {
        val groupId = request.groupId
        if (groupId != null &&
            !groupPermissionService.hasPermission(
                userId = userId,
                groupId = groupId,
            )
        ) {
            return emptyList()
        }

        val filter = request.filterWithDefault
        val generated =
            if (filter == ScheduledExecutionFilter.ALL || filter == ScheduledExecutionFilter.ALREADY_GENERATED) {
                listGenerated(userId = userId, groupId = groupId)
            } else {
                emptyList()
            }
        val futures =
            if (filter == ScheduledExecutionFilter.ALL || filter == ScheduledExecutionFilter.FUTURE) {
                listFutures(userId = userId, groupId = groupId)
            } else {
                emptyList()
            }

        return (generated + futures).sortedWith(
            compareByDescending<EventListResponse> { it.date }
                .thenByDescending { it.id },
        )
    }

    private suspend fun listGenerated(
        userId: UUID,
        groupId: UUID?,
    ): List<EventListResponse> {
        val events =
            walletEventRepository
                .findAll(
                    userId = if (groupId == null) userId else null,
                    groupId = groupId,
                    limit = maxGeneratedItems,
                    walletItemId = null,
                    minimumDate = null,
                    maximumDate = null,
                    billId = null,
                    cursor = null,
                ).collectList()
                .awaitSingle()
                .filter { it.recurrenceEventId != null }

        return walletEventListService.convertEntityToEntryListResponse(events, false)
    }

    private suspend fun listFutures(
        userId: UUID,
        groupId: UUID?,
    ): List<EventListResponse> =
        recurrenceSimulationService.simulateGeneration(
            minimumEndExecution = LocalDate.now(clock).plusDays(1),
            maximumNextExecution = null,
            userId = if (groupId == null) userId else null,
            groupId = groupId,
            walletItemId = null,
            billDate = null,
        )
}
