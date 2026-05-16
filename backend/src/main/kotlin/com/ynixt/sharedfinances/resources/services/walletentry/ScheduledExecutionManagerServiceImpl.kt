package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.enums.ScheduledExecutionFilter
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.ScheduledExecutionManagerRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.ScheduledExecutionManagerService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
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
        val selectedMonth = parseSelectedMonth(request.selectedMonth)
        val generated =
            if (filter == ScheduledExecutionFilter.ALL || filter == ScheduledExecutionFilter.ALREADY_GENERATED) {
                listGenerated(userId = userId, groupId = groupId, selectedMonth = selectedMonth)
            } else {
                emptyList()
            }
        val futures =
            if (filter == ScheduledExecutionFilter.ALL || filter == ScheduledExecutionFilter.FUTURE) {
                listFutures(userId = userId, groupId = groupId, selectedMonth = selectedMonth)
            } else {
                emptyList()
            }

        return (generated + futures).sortedWith(
            compareBy<EventListResponse> { it.date }
                .thenBy { it.id },
        )
    }

    private suspend fun listGenerated(
        userId: UUID,
        groupId: UUID?,
        selectedMonth: YearMonth,
    ): List<EventListResponse> {
        val startDate = selectedMonth.atDay(1)
        val endDate = selectedMonth.atEndOfMonth()
        val events =
            walletEventRepository
                .findAll(
                    scope =
                        if (groupId == null) {
                            WalletTransactionQueryScope.ownership(ownerUserIds = setOf(userId))
                        } else {
                            WalletTransactionQueryScope.group(groupIds = setOf(groupId))
                        },
                    limit = maxGeneratedItems,
                    walletItemId = null,
                    minimumDate = startDate,
                    maximumDate = endDate,
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
        selectedMonth: YearMonth,
    ): List<EventListResponse> =
        if (selectedMonth.isBefore(YearMonth.from(LocalDate.now(clock)))) {
            emptyList()
        } else {
            recurrenceSimulationService.simulateGeneration(
                minimumEndExecution = maxOf(LocalDate.now(clock).plusDays(1), selectedMonth.atDay(1)),
                maximumNextExecution = selectedMonth.atEndOfMonth(),
                userId = if (groupId == null) userId else null,
                groupIds = if (groupId == null) emptySet() else setOfNotNull(groupId),
                walletItemId = null,
                billDate = null,
            )
        }

    private fun parseSelectedMonth(value: String?): YearMonth =
        value
            ?.let { raw -> runCatching { YearMonth.parse(raw) }.getOrNull() }
            ?: YearMonth.now(clock)
}
