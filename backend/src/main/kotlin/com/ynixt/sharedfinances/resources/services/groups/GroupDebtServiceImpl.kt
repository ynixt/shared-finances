package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtMovementNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidDebtSettlementException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupDebtAdjustmentException
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyComposition
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairBalance
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairHistory
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.walletentry.WalletSourceSplit
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class GroupDebtServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val movementRepository: GroupMemberDebtMovementSpringDataRepository,
    private val debtDatabaseClientRepository: GroupMemberDebtDatabaseClientRepository,
    private val walletEventRepository: WalletEventRepository,
    private val walletEventListService: WalletEventListService,
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val clock: Clock,
) : GroupDebtService {
    companion object {
        private val ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    }

    @Transactional
    override suspend fun applyWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ) {
        persistMovements(
            deriveMovements(
                actorUserId = actorUserId,
                event = event,
                entries = entries,
            ),
        )
    }

    @Transactional
    override suspend fun rollbackWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
    ) {
        val eventId = event.id ?: return
        val reversals =
            movementRepository
                .findActiveBySourceWalletEventId(eventId)
                .collectList()
                .awaitSingle()
                .mapNotNull { source -> source.toReversal(actorUserId) }

        persistMovements(reversals)
    }

    override suspend fun getWorkspace(
        userId: UUID,
        groupId: UUID,
    ): GroupDebtWorkspace {
        ensureReadAccess(userId, groupId)

        val rows =
            debtDatabaseClientRepository
                .listMonthlyComposition(groupId)
                .collectList()
                .awaitSingle()

        return GroupDebtWorkspace(balances = mapBalances(rows))
    }

    override suspend fun getWorkspaceForMonth(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
    ): GroupDebtWorkspace {
        ensureReadAccess(userId, groupId)

        val persistedRows = loadPersistedMonthlyCompositionRows(groupId)
        return GroupDebtWorkspace(
            balances =
                mapBalances(
                    rows =
                        loadWorkspaceRowsForSelectedMonth(
                            userId = userId,
                            groupId = groupId,
                            selectedMonth = selectedMonth,
                            persistedRows = persistedRows,
                        ),
                    includeZeroBalances = true,
                ),
        )
    }

    override suspend fun listHistory(
        userId: UUID,
        groupId: UUID,
        filter: GroupDebtHistoryFilter,
    ): List<GroupDebtMovementLine> {
        ensureReadAccess(userId, groupId)

        val history =
            debtDatabaseClientRepository
                .listHistory(
                    groupId = groupId,
                    payerId = filter.payerId,
                    receiverId = filter.receiverId,
                    currency = filter.currency,
                    selectedMonth = filter.selectedMonth,
                ).collectList()
                .awaitSingle()
                .map { row -> row.toLine() }

        val hydratedHistory = hydrateSourceWalletEvents(history)
        val projectedHistory =
            loadProjectedMovementLinesForMonth(
                userId = userId,
                groupId = groupId,
                selectedMonth = filter.selectedMonth,
                payerId = filter.payerId,
                receiverId = filter.receiverId,
                currency = filter.currency,
            )

        return sortHistoryLines(hydratedHistory + projectedHistory)
    }

    override suspend fun listPairHistory(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
    ): List<GroupDebtPairHistory> {
        ensureReadAccess(userId, groupId)

        val rows =
            loadWorkspaceRowsForSelectedMonth(
                userId = userId,
                groupId = groupId,
                selectedMonth = selectedMonth,
                persistedRows = loadPersistedMonthlyCompositionRows(groupId),
            )
        val directionalBalances = mapBalances(rows = rows, includeZeroBalances = true)
        val historyLines =
            listHistory(
                userId = userId,
                groupId = groupId,
                filter = GroupDebtHistoryFilter(selectedMonth = selectedMonth),
            )
        val historyByDirection =
            historyLines.groupBy { line ->
                DirectionalDebtKey(
                    payerId = line.payerId,
                    receiverId = line.receiverId,
                    currency = line.currency.uppercase(),
                )
            }
        val balancesByDirection =
            directionalBalances.associateBy { balance ->
                DirectionalDebtKey(
                    payerId = balance.payerId,
                    receiverId = balance.receiverId,
                    currency = balance.currency.uppercase(),
                )
            }

        val canonicalKeys =
            (
                directionalBalances.map { balance ->
                    canonicalDebtKeyFor(balance.payerId, balance.receiverId, balance.currency)
                } +
                    historyByDirection.keys.map { key ->
                        canonicalDebtKeyFor(key.payerId, key.receiverId, key.currency)
                    }
            ).toSet()

        return canonicalKeys
            .mapNotNull { canonicalKey ->
                val forward = DirectionalDebtKey(canonicalKey.firstUserId, canonicalKey.secondUserId, canonicalKey.currency)
                val backward = DirectionalDebtKey(canonicalKey.secondUserId, canonicalKey.firstUserId, canonicalKey.currency)
                val forwardBalance = balancesByDirection[forward]
                val backwardBalance = balancesByDirection[backward]
                val lines =
                    sortHistoryLines(
                        directionalLinesForPair(
                            userId = userId,
                            groupId = groupId,
                            selectedMonth = selectedMonth,
                            direction = forward,
                            historyByDirection = historyByDirection,
                        ) +
                            directionalLinesForPair(
                                userId = userId,
                                groupId = groupId,
                                selectedMonth = selectedMonth,
                                direction = backward,
                                historyByDirection = historyByDirection,
                            ),
                    )

                if (forwardBalance == null && backwardBalance == null && lines.isEmpty()) {
                    return@mapNotNull null
                }

                val forwardComposition = forwardBalance?.monthlyComposition?.firstOrNull()
                val backwardComposition = backwardBalance?.monthlyComposition?.firstOrNull()
                val forwardOutstanding = forwardBalance?.outstandingAmount ?: ZERO
                val backwardOutstanding = backwardBalance?.outstandingAmount ?: ZERO
                val netSigned = forwardOutstanding.subtract(backwardOutstanding).asMoney()
                val directionMultiplier =
                    when {
                        netSigned.compareTo(ZERO) > 0 -> BigDecimal.ONE
                        netSigned.compareTo(ZERO) < 0 -> BigDecimal.ONE.negate()
                        else -> BigDecimal.ONE
                    }

                GroupDebtPairHistory(
                    firstUserId = canonicalKey.firstUserId,
                    secondUserId = canonicalKey.secondUserId,
                    currency = canonicalKey.currency,
                    month = selectedMonth,
                    netPayerId =
                        when {
                            netSigned.compareTo(ZERO) > 0 -> forward.payerId
                            netSigned.compareTo(ZERO) < 0 -> backward.payerId
                            else -> null
                        },
                    netReceiverId =
                        when {
                            netSigned.compareTo(ZERO) > 0 -> forward.receiverId
                            netSigned.compareTo(ZERO) < 0 -> backward.receiverId
                            else -> null
                        },
                    netAmount = netSigned.abs().asMoney(),
                    chargeDelta =
                        netDirectionalAmount(
                            forwardAmount = forwardComposition?.chargeDelta ?: ZERO,
                            backwardAmount = backwardComposition?.chargeDelta ?: ZERO,
                            directionMultiplier = directionMultiplier,
                        ),
                    settlementDelta =
                        netDirectionalAmount(
                            forwardAmount = forwardComposition?.settlementDelta ?: ZERO,
                            backwardAmount = backwardComposition?.settlementDelta ?: ZERO,
                            directionMultiplier = directionMultiplier,
                        ),
                    manualAdjustmentDelta =
                        netDirectionalAmount(
                            forwardAmount = forwardComposition?.manualAdjustmentDelta ?: ZERO,
                            backwardAmount = backwardComposition?.manualAdjustmentDelta ?: ZERO,
                            directionMultiplier = directionMultiplier,
                        ),
                    lines = lines,
                )
            }.sortedWith(
                compareBy<GroupDebtPairHistory> { it.firstUserId.toString() }
                    .thenBy { it.secondUserId.toString() }
                    .thenBy { it.currency },
            )
    }

    override suspend fun getMovement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ): GroupDebtMovementLine {
        ensureReadAccess(userId, groupId)

        val movement =
            movementRepository
                .findByIdAndGroupId(movementId, groupId)
                .awaitSingleOrNull()
                ?.toLine() ?: throw GroupDebtMovementNotFoundException(movementId)

        return hydrateSourceWalletEvents(listOf(movement)).first()
    }

    @Transactional
    override suspend fun createManualAdjustment(
        userId: UUID,
        groupId: UUID,
        input: NewGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine {
        ensureMutationAccess(userId, groupId)
        validateManualAdjustmentInput(input.payerId, input.receiverId, input.currency, input.amountDelta)

        val movement =
            GroupMemberDebtMovementEntity(
                groupId = groupId,
                payerId = input.payerId,
                receiverId = input.receiverId,
                month = input.month.atDay(1),
                currency = input.currency.uppercase(),
                deltaSigned = input.amountDelta.asMoney(),
                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT,
                createdByUserId = userId,
                note = input.note?.trim()?.ifBlank { null },
            )

        return persistMovements(listOf(movement)).single().toLine()
    }

    @Transactional
    override suspend fun editManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
        input: EditGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine {
        ensureMutationAccess(userId, groupId)
        val root =
            movementRepository
                .findByIdAndGroupId(movementId, groupId)
                .awaitSingleOrNull() ?: throw GroupDebtMovementNotFoundException(movementId)

        if (root.reasonKind != GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT) {
            throw InvalidGroupDebtAdjustmentException("Only root manual adjustments can be edited")
        }

        val targetAmount = input.amountDelta.asMoney()
        if (targetAmount.compareTo(ZERO) == 0) {
            throw InvalidGroupDebtAdjustmentException("Manual adjustment amount delta must be different from zero")
        }

        val currentNet =
            movementRepository
                .findAdjustmentChain(root.id!!)
                .collectList()
                .awaitSingle()
                .filter { movement ->
                    movement.reasonKind == GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT ||
                        movement.reasonKind == GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT_COMPENSATION
                }.fold(ZERO) { acc, movement ->
                    acc.add(movement.deltaSigned).asMoney()
                }

        val compensation = targetAmount.subtract(currentNet).asMoney()
        if (compensation.compareTo(ZERO) == 0) {
            return root.toLine()
        }

        val compensationMovement =
            GroupMemberDebtMovementEntity(
                groupId = root.groupId,
                payerId = root.payerId,
                receiverId = root.receiverId,
                month = root.month,
                currency = root.currency.uppercase(),
                deltaSigned = compensation,
                reasonKind = GroupDebtMovementReasonKind.MANUAL_ADJUSTMENT_COMPENSATION,
                createdByUserId = userId,
                note = input.note?.trim()?.ifBlank { root.note },
                sourceMovementId = root.id,
            )

        return persistMovements(listOf(compensationMovement)).single().toLine()
    }

    override suspend fun loadMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow> {
        if (scopedUserIds.isEmpty() || fromMonth.isAfter(toMonth)) {
            return emptyMap()
        }

        return debtDatabaseClientRepository
            .summarizeMonthlyCashFlow(
                groupId = groupId,
                scopedUserIds = scopedUserIds,
                fromMonth = fromMonth.atDay(1),
                toMonth = toMonth.atDay(1),
            ).collectList()
            .awaitSingle()
            .associate { row ->
                (row.month to row.currency.uppercase()) to
                    GroupDebtMonthlyCashFlow(
                        debtOutflow = row.debtOutflow.asMoney(),
                        debtInflow = row.debtInflow.asMoney(),
                    )
            }
    }

    private suspend fun loadProjectedMonthlyRows(
        userId: UUID,
        groupId: UUID,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow> {
        val projectedByPairAndCurrency =
            loadProjectedMovementLinesForMonths(
                userId = userId,
                groupId = groupId,
                fromMonth = fromMonth,
                toMonth = toMonth,
            ).groupBy { line ->
                DebtRowKey(
                    payerId = line.payerId,
                    receiverId = line.receiverId,
                    currency = line.currency.uppercase(),
                    month = line.month,
                )
            }

        return projectedByPairAndCurrency.entries
            .asSequence()
            .map { (key, lines) ->
                val amount =
                    lines.fold(ZERO) { acc, line ->
                        acc.add(line.deltaSigned).asMoney()
                    }
                GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow(
                    payerId = key.payerId,
                    receiverId = key.receiverId,
                    currency = key.currency,
                    month = key.month,
                    netAmount = amount.asMoney(),
                    chargeDelta = amount.asMoney(),
                    settlementDelta = ZERO,
                    manualAdjustmentDelta = ZERO,
                )
            }.filter { row -> row.netAmount.compareTo(ZERO) > 0 }
            .toList()
    }

    private suspend fun loadPersistedMonthlyCompositionRows(
        groupId: UUID,
    ): List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow> =
        debtDatabaseClientRepository
            .listMonthlyComposition(groupId)
            .collectList()
            .awaitSingle()

    private suspend fun loadWorkspaceRowsForSelectedMonth(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
        persistedRows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
    ): List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow> {
        val currentMonth = YearMonth.from(LocalDate.now(clock))
        val rowsForSelectedMonth =
            persistedRows
                .filter { row -> row.month == selectedMonth }
                .toMutableList()

        val projectedRows =
            if (shouldIncludeProjectedForMonth(selectedMonth)) {
                loadProjectedMonthlyRows(
                    userId = userId,
                    groupId = groupId,
                    fromMonth = currentMonth,
                    toMonth = selectedMonth,
                )
            } else {
                emptyList()
            }

        mergeCarriedOverBalancesIntoSelectedMonth(
            carryoverRows =
                persistedRows.filter { row -> row.month.isBefore(selectedMonth) } +
                    projectedRows.filter { row -> row.month.isBefore(selectedMonth) },
            selectedMonth = selectedMonth,
            rowsForSelectedMonth = rowsForSelectedMonth,
        )

        rowsForSelectedMonth += projectedRows.filter { row -> row.month == selectedMonth }

        return rowsForSelectedMonth
    }

    private fun mergeCarriedOverBalancesIntoSelectedMonth(
        carryoverRows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
        selectedMonth: YearMonth,
        rowsForSelectedMonth: MutableList<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
    ) {
        val carryoverByPair =
            carryoverRows
                .asSequence()
                .filter { row -> row.month.isBefore(selectedMonth) && row.netAmount.compareTo(ZERO) > 0 }
                .groupBy { row -> Triple(row.payerId, row.receiverId, row.currency.uppercase()) }
                .mapValues { (_, rows) ->
                    rows.fold(ZERO) { acc, row -> acc.add(row.netAmount).asMoney() }
                }

        carryoverByPair.forEach { (key, carryoverAmount) ->
            if (carryoverAmount.compareTo(ZERO) <= 0) {
                return@forEach
            }

            val existingIndex =
                rowsForSelectedMonth.indexOfFirst { row ->
                    row.payerId == key.first &&
                        row.receiverId == key.second &&
                        row.currency.equals(key.third, ignoreCase = true)
                }

            if (existingIndex >= 0) {
                val existing = rowsForSelectedMonth[existingIndex]
                rowsForSelectedMonth[existingIndex] =
                    existing.copy(netAmount = existing.netAmount.add(carryoverAmount).asMoney())
            } else {
                rowsForSelectedMonth +=
                    GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow(
                        payerId = key.first,
                        receiverId = key.second,
                        currency = key.third,
                        month = selectedMonth,
                        netAmount = carryoverAmount.asMoney(),
                        chargeDelta = ZERO,
                        settlementDelta = ZERO,
                        manualAdjustmentDelta = ZERO,
                    )
            }
        }
    }

    private suspend fun loadProjectedMovementLinesForMonth(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth?,
        payerId: UUID? = null,
        receiverId: UUID? = null,
        currency: String? = null,
    ): List<GroupDebtMovementLine> =
        selectedMonth?.let { month ->
            loadProjectedMovementLinesForMonths(
                userId = userId,
                groupId = groupId,
                fromMonth = month,
                toMonth = month,
                payerId = payerId,
                receiverId = receiverId,
                currency = currency,
            )
        } ?: emptyList()

    private suspend fun loadProjectedMovementLinesForMonths(
        userId: UUID,
        groupId: UUID,
        fromMonth: YearMonth,
        toMonth: YearMonth,
        payerId: UUID? = null,
        receiverId: UUID? = null,
        currency: String? = null,
    ): List<GroupDebtMovementLine> {
        val currentMonth = YearMonth.from(LocalDate.now(clock))
        if (fromMonth.isAfter(toMonth) || toMonth.isBefore(currentMonth)) {
            return emptyList()
        }

        val today = LocalDate.now(clock)
        val effectiveFromMonth = maxOf(fromMonth, currentMonth)
        val selectedMonthEnd = toMonth.atEndOfMonth()
        if (selectedMonthEnd.isBefore(today)) {
            return emptyList()
        }

        val projectedEvents =
            recurrenceSimulationService.simulateGenerationWithFilters(
                minimumEndExecution = projectedSimulationStartDate(effectiveFromMonth),
                maximumNextExecution = selectedMonthEnd,
                userId = userId,
                walletItemId = null,
                billDate = null,
                groupIds = setOf(groupId),
                userIds = emptySet(),
                walletItemIds = emptySet(),
                entryTypes = setOf(WalletEntryType.REVENUE, WalletEntryType.EXPENSE),
                categoryConceptIds = emptySet(),
                includeUncategorized = false,
            )

        return projectedEvents
            .withIndex()
            .asSequence()
            .flatMap { (eventIndex, event) ->
                deriveProjectedDebtMovementsFromEvent(event)
                    .withIndex()
                    .asSequence()
                    .filter { (_, projected) ->
                        projected.amount.compareTo(ZERO) > 0 &&
                            !projected.month.isBefore(effectiveFromMonth) &&
                            !projected.month.isAfter(toMonth) &&
                            (payerId == null || projected.payerId == payerId) &&
                            (receiverId == null || projected.receiverId == receiverId) &&
                            (currency.isNullOrBlank() || projected.currency.equals(currency, ignoreCase = true))
                    }.map { (movementIndex, projected) ->
                        projected.toHistoryLine(
                            event = event,
                            eventIndex = eventIndex,
                            movementIndex = movementIndex,
                        )
                    }
            }.toList()
    }

    private fun mapBalances(
        rows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>,
        includeZeroBalances: Boolean = false,
    ): List<GroupDebtPairBalance> =
        rows
            .groupBy { row -> Triple(row.payerId, row.receiverId, row.currency.uppercase()) }
            .mapNotNull { (key, pairRows) ->
                val monthlyComposition =
                    pairRows
                        .groupBy { row -> row.month }
                        .map { (month, monthRows) ->
                            GroupDebtMonthlyComposition(
                                month = month,
                                netAmount = monthRows.fold(ZERO) { acc, row -> acc.add(row.netAmount).asMoney() },
                                chargeDelta = monthRows.fold(ZERO) { acc, row -> acc.add(row.chargeDelta).asMoney() },
                                settlementDelta = monthRows.fold(ZERO) { acc, row -> acc.add(row.settlementDelta).asMoney() },
                                manualAdjustmentDelta = monthRows.fold(ZERO) { acc, row -> acc.add(row.manualAdjustmentDelta).asMoney() },
                            )
                        }.sortedBy { month -> month.month }
                        .filter { month ->
                            month.netAmount.compareTo(ZERO) != 0 ||
                                month.chargeDelta.compareTo(ZERO) != 0 ||
                                month.settlementDelta.compareTo(ZERO) != 0 ||
                                month.manualAdjustmentDelta.compareTo(ZERO) != 0
                        }

                val outstanding =
                    monthlyComposition
                        .fold(ZERO) { acc, row -> acc.add(row.netAmount).asMoney() }

                if (outstanding.compareTo(ZERO) == 0) {
                    if (!includeZeroBalances) {
                        null
                    } else {
                        GroupDebtPairBalance(
                            payerId = key.first,
                            receiverId = key.second,
                            currency = key.third,
                            outstandingAmount = outstanding,
                            monthlyComposition = monthlyComposition,
                        )
                    }
                } else {
                    GroupDebtPairBalance(
                        payerId = key.first,
                        receiverId = key.second,
                        currency = key.third,
                        outstandingAmount = outstanding,
                        monthlyComposition = monthlyComposition,
                    )
                }
            }.sortedWith(
                compareBy<GroupDebtPairBalance> { it.payerId.toString() }
                    .thenBy { it.receiverId.toString() }
                    .thenBy { it.currency },
            )

    private fun shouldIncludeProjectedForMonth(selectedMonth: YearMonth): Boolean =
        !selectedMonth.isBefore(YearMonth.from(LocalDate.now(clock)))

    private suspend fun directionalLinesForPair(
        userId: UUID,
        groupId: UUID,
        selectedMonth: YearMonth,
        direction: DirectionalDebtKey,
        historyByDirection: Map<DirectionalDebtKey, List<GroupDebtMovementLine>>,
    ): List<GroupDebtMovementLine> =
        historyByDirection[direction].orEmpty() +
            loadCarriedOverOpenBalanceLines(
                userId = userId,
                groupId = groupId,
                payerId = direction.payerId,
                receiverId = direction.receiverId,
                currency = direction.currency,
                selectedMonth = selectedMonth,
            )

    private suspend fun loadCarriedOverOpenBalanceLines(
        userId: UUID,
        groupId: UUID,
        payerId: UUID,
        receiverId: UUID,
        currency: String,
        selectedMonth: YearMonth,
    ): List<GroupDebtMovementLine> {
        val persistedCarryoverLines =
            loadPersistedMonthlyCompositionRows(groupId)
                .asSequence()
                .filter { row ->
                    row.payerId == payerId &&
                        row.receiverId == receiverId &&
                        row.currency.equals(currency, ignoreCase = true) &&
                        row.month.isBefore(selectedMonth) &&
                        row.netAmount.compareTo(ZERO) > 0
                }.sortedBy { row -> row.month }
                .map { row -> row.toCarriedOverLine() }
                .toList()

        val projectedCarryoverLines =
            if (shouldIncludeProjectedForMonth(selectedMonth)) {
                loadProjectedMovementLinesForMonths(
                    userId = userId,
                    groupId = groupId,
                    fromMonth = YearMonth.from(LocalDate.now(clock)),
                    toMonth = selectedMonth.minusMonths(1),
                    payerId = payerId,
                    receiverId = receiverId,
                    currency = currency,
                ).map { line -> line.toCarriedOverLine() }
            } else {
                emptyList()
            }

        return aggregateCarriedOverLinesByMonth(persistedCarryoverLines + projectedCarryoverLines)
    }

    private fun aggregateCarriedOverLinesByMonth(lines: List<GroupDebtMovementLine>): List<GroupDebtMovementLine> =
        lines
            .groupBy { line -> line.month }
            .entries
            .sortedBy { (month, _) -> month }
            .map { (month, monthLines) ->
                val first = monthLines.first()
                val total =
                    monthLines.fold(ZERO) { acc, line ->
                        acc.add(line.deltaSigned).asMoney()
                    }

                val tempId = "carryover-grouped|${first.payerId}|${first.receiverId}|${first.currency.uppercase()}|$month|$total|${
                    monthLines.any {
                        it.projected
                    }
                }"

                GroupDebtMovementLine(
                    id = UUID.nameUUIDFromBytes(tempId.toByteArray()),
                    payerId = first.payerId,
                    receiverId = first.receiverId,
                    month = month,
                    transactionDate = null,
                    currency = first.currency.uppercase(),
                    deltaSigned = total,
                    reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
                    createdByUserId = first.createdByUserId,
                    carriedOver = true,
                    projected = monthLines.any { it.projected },
                    note = null,
                    sourceWalletEventId = null,
                    sourceWalletEvent = null,
                    sourceMovementId = null,
                    createdAt = null,
                )
            }

    private fun projectedSimulationStartDate(fromMonth: YearMonth): LocalDate = fromMonth.atDay(1).minusMonths(1)

    private fun canonicalDebtKeyFor(
        userA: UUID,
        userB: UUID,
        currency: String,
    ): CanonicalDebtKey =
        if (userA.toString() <= userB.toString()) {
            CanonicalDebtKey(firstUserId = userA, secondUserId = userB, currency = currency.uppercase())
        } else {
            CanonicalDebtKey(firstUserId = userB, secondUserId = userA, currency = currency.uppercase())
        }

    private fun netDirectionalAmount(
        forwardAmount: BigDecimal,
        backwardAmount: BigDecimal,
        directionMultiplier: BigDecimal,
    ): BigDecimal = forwardAmount.subtract(backwardAmount).multiply(directionMultiplier).asMoney()

    private fun sortHistoryLines(movements: List<GroupDebtMovementLine>): List<GroupDebtMovementLine> =
        movements.sortedWith(
            compareBy<GroupDebtMovementLine>({
                it.transactionDate ?: it.createdAt?.toLocalDate() ?: it.month.atDay(1)
            }, { it.createdAt }, { it.projected })
                .thenBy { it.id.toString() },
        )

    private suspend fun persistMovements(movements: List<GroupMemberDebtMovementEntity>): List<GroupMemberDebtMovementEntity> {
        if (movements.isEmpty()) {
            return emptyList()
        }

        return buildList {
            for (movement in movements) {
                val saved = movementRepository.save(movement).awaitSingle()
                debtDatabaseClientRepository.upsertMonthlyDelta(saved).awaitSingleOrNull()
                add(saved)
            }
        }
    }

    private data class DirectionalDebtKey(
        val payerId: UUID,
        val receiverId: UUID,
        val currency: String,
    )

    private data class CanonicalDebtKey(
        val firstUserId: UUID,
        val secondUserId: UUID,
        val currency: String,
    )

    private suspend fun deriveMovements(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ): List<GroupMemberDebtMovementEntity> {
        val groupId = event.groupId ?: return emptyList()
        if (entries.isEmpty()) {
            return emptyList()
        }

        return when {
            event.type == WalletEntryType.TRANSFER && event.transferPurpose == TransferPurpose.DEBT_SETTLEMENT ->
                deriveSettlementMovements(actorUserId, groupId, event, entries)

            event.type != WalletEntryType.TRANSFER ->
                deriveBeneficiaryChargeMovements(
                    actorUserId = actorUserId,
                    groupId = groupId,
                    event = event,
                    entries = entries,
                )

            else -> emptyList()
        }
    }

    private suspend fun deriveSettlementMovements(
        actorUserId: UUID,
        groupId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ): List<GroupMemberDebtMovementEntity> {
        val origin = entries.firstOrNull { it.value < ZERO } ?: entries.first()
        val target = entries.firstOrNull { it.walletItemId != origin.walletItemId } ?: entries.last()
        val originOwner = requireNotNull(origin.walletItem?.userId) { "Settlement origin wallet owner must be hydrated" }
        val targetOwner = requireNotNull(target.walletItem?.userId) { "Settlement target wallet owner must be hydrated" }
        val currency = requireNotNull(origin.walletItem?.currency).uppercase()
        val paymentAmount = origin.value.abs().asMoney()
        val openBalances =
            debtDatabaseClientRepository
                .listOpenBalancesForPair(
                    groupId = groupId,
                    payerId = originOwner,
                    receiverId = targetOwner,
                    currency = currency,
                ).collectList()
                .awaitSingle()

        if (paymentAmount.compareTo(ZERO) <= 0) {
            throw InvalidDebtSettlementException("Debt settlement amount must be greater than zero")
        }
        if (openBalances.isEmpty()) {
            throw InvalidDebtSettlementException("No open debt exists for the selected settlement pair")
        }

        var remaining = paymentAmount
        val movements = mutableListOf<GroupMemberDebtMovementEntity>()
        openBalances.forEach { openBalance ->
            if (remaining.compareTo(ZERO) <= 0) {
                return@forEach
            }

            val allocation =
                openBalance.balance
                    .asMoney()
                    .min(remaining)
                    .asMoney()
            if (allocation.compareTo(ZERO) <= 0) {
                return@forEach
            }

            movements +=
                GroupMemberDebtMovementEntity(
                    groupId = groupId,
                    payerId = originOwner,
                    receiverId = targetOwner,
                    month = openBalance.month.atDay(1),
                    currency = currency,
                    deltaSigned = allocation.negate().asMoney(),
                    reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                    createdByUserId = actorUserId,
                    sourceWalletEventId = event.id,
                )
            remaining = remaining.subtract(allocation).asMoney()
        }

        if (remaining.compareTo(ZERO) > 0) {
            throw InvalidDebtSettlementException("Debt settlement amount exceeds open debt for the selected pair")
        }

        return movements
    }

    private fun deriveBeneficiaryChargeMovements(
        actorUserId: UUID,
        groupId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ): List<GroupMemberDebtMovementEntity> {
        val beneficiaries =
            event.beneficiaries
                ?.takeIf { it.isNotEmpty() }
                ?: return emptyList()

        val actualByUser =
            entries.fold(linkedMapOf<UUID, BigDecimal>()) { acc, entry ->
                val userId = requireNotNull(entry.walletItem?.userId) { "Wallet entry wallet item must be hydrated" }
                acc[userId] = acc.getOrDefault(userId, ZERO).add(entry.value).asMoney()
                acc
            }

        val totalMagnitude =
            entries
                .fold(ZERO) { acc, entry -> acc.add(entry.value).asMoney() }
                .abs()

        val benefitValues =
            WalletSourceSplit.distributeLegValues(
                type = event.type,
                totalMagnitude = totalMagnitude,
                percents = beneficiaries.map { beneficiary -> beneficiary.benefitPercent },
            )
        val benefitByUser = linkedMapOf<UUID, BigDecimal>()
        beneficiaries.zip(benefitValues).forEach { (beneficiary, share) ->
            benefitByUser[beneficiary.beneficiaryUserId] =
                benefitByUser.getOrDefault(beneficiary.beneficiaryUserId, ZERO).add(share).asMoney()
        }

        val netByUser =
            (actualByUser.keys + benefitByUser.keys)
                .associateWith { userId ->
                    actualByUser
                        .getOrDefault(userId, ZERO)
                        .subtract(benefitByUser.getOrDefault(userId, ZERO))
                        .asMoney()
                }

        val debtors =
            netByUser
                .filterValues { value -> value.compareTo(ZERO) > 0 }
                .map { MutablePosition(it.key, it.value.asMoney()) }
                .sortedBy { it.userId.toString() }
                .toMutableList()
        val creditors =
            netByUser
                .filterValues { value -> value.compareTo(ZERO) < 0 }
                .map { MutablePosition(it.key, it.value.asMoney()) }
                .sortedBy { it.userId.toString() }
                .toMutableList()

        val currency = requireNotNull(entries.first().walletItem?.currency).uppercase()
        val month = deriveDebtMonth(event.date, entries)
        val result = mutableListOf<GroupMemberDebtMovementEntity>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val delta = debtor.remaining.min(creditor.remaining.abs()).asMoney()

            if (delta.compareTo(ZERO) > 0 && debtor.userId != creditor.userId) {
                result +=
                    GroupMemberDebtMovementEntity(
                        groupId = groupId,
                        payerId = debtor.userId,
                        receiverId = creditor.userId,
                        month = month,
                        currency = currency,
                        deltaSigned = delta,
                        reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
                        createdByUserId = actorUserId,
                        sourceWalletEventId = event.id,
                    )
            }

            debtor.remaining = debtor.remaining.subtract(delta).asMoney()
            creditor.remaining = creditor.remaining.add(delta).asMoney()

            if (debtor.remaining.compareTo(ZERO) == 0) {
                debtorIndex++
            }
            if (creditor.remaining.compareTo(ZERO) == 0) {
                creditorIndex++
            }
        }

        return result
    }

    private suspend fun ensureReadAccess(
        userId: UUID,
        groupId: UUID,
    ) {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = groupId)) {
            throw GroupDebtForbiddenException()
        }
    }

    private suspend fun ensureMutationAccess(
        userId: UUID,
        groupId: UUID,
    ) {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = groupId, permission = GroupPermissions.SEND_ENTRIES)) {
            throw GroupDebtForbiddenException()
        }
    }

    private fun validateManualAdjustmentInput(
        payerId: UUID,
        receiverId: UUID,
        currency: String,
        amountDelta: BigDecimal,
    ) {
        if (payerId == receiverId) {
            throw InvalidGroupDebtAdjustmentException("Payer and receiver must be different users")
        }
        if (currency.isBlank()) {
            throw InvalidGroupDebtAdjustmentException("Currency is required")
        }
        if (amountDelta.asMoney().compareTo(ZERO) == 0) {
            throw InvalidGroupDebtAdjustmentException("Manual adjustment amount delta must be different from zero")
        }
    }

    private fun GroupMemberDebtMovementEntity.toReversal(actorUserId: UUID): GroupMemberDebtMovementEntity? {
        val reversalKind =
            when (reasonKind) {
                GroupDebtMovementReasonKind.BENEFICIARY_CHARGE -> GroupDebtMovementReasonKind.BENEFICIARY_REVERSAL
                GroupDebtMovementReasonKind.DEBT_SETTLEMENT -> GroupDebtMovementReasonKind.DEBT_SETTLEMENT_REVERSAL
                else -> null
            } ?: return null

        return GroupMemberDebtMovementEntity(
            groupId = groupId,
            payerId = payerId,
            receiverId = receiverId,
            month = month,
            currency = currency.uppercase(),
            deltaSigned = deltaSigned.negate().asMoney(),
            reasonKind = reversalKind,
            createdByUserId = actorUserId,
            sourceMovementId = id,
        )
    }

    private fun GroupMemberDebtMovementEntity.toLine(): GroupDebtMovementLine =
        GroupDebtMovementLine(
            id = requireNotNull(id),
            payerId = payerId,
            receiverId = receiverId,
            month = YearMonth.from(month),
            transactionDate = null,
            currency = currency.uppercase(),
            deltaSigned = deltaSigned.asMoney(),
            reasonKind = reasonKind,
            createdByUserId = createdByUserId,
            carriedOver = false,
            projected = false,
            note = note,
            sourceWalletEventId = sourceWalletEventId,
            sourceMovementId = sourceMovementId,
            createdAt = createdAt,
        )

    private fun GroupMemberDebtDatabaseClientRepository.DebtMovementHistoryRow.toLine(): GroupDebtMovementLine =
        GroupDebtMovementLine(
            id = id,
            payerId = payerId,
            receiverId = receiverId,
            month = month,
            transactionDate = null,
            currency = currency.uppercase(),
            deltaSigned = deltaSigned.asMoney(),
            reasonKind = GroupDebtMovementReasonKind.valueOf(reasonKind),
            createdByUserId = createdByUserId,
            carriedOver = false,
            projected = false,
            note = note,
            sourceWalletEventId = sourceWalletEventId,
            sourceMovementId = sourceMovementId,
            createdAt = createdAt,
        )

    private fun ProjectedDebtMovement.toHistoryLine(
        event: com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse,
        eventIndex: Int,
        movementIndex: Int,
    ): GroupDebtMovementLine =
        GroupDebtMovementLine(
            id =
                UUID.nameUUIDFromBytes(
                    "$month|${event.date}|$eventIndex|$movementIndex|$payerId|$receiverId|${currency.uppercase()}|${amount.asMoney()}"
                        .toByteArray(),
                ),
            payerId = payerId,
            receiverId = receiverId,
            month = month,
            transactionDate = event.date,
            currency = currency.uppercase(),
            deltaSigned = amount.asMoney(),
            reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
            createdByUserId =
                event.user?.id ?: event.entries
                    .firstOrNull()
                    ?.walletItem
                    ?.userId ?: receiverId,
            carriedOver = false,
            projected = true,
            note = event.observations,
            sourceWalletEventId = null,
            sourceWalletEvent = event,
            sourceMovementId = null,
            createdAt = null,
        )

    private fun GroupDebtMovementLine.toCarriedOverLine(): GroupDebtMovementLine =
        copy(
            id = UUID.nameUUIDFromBytes("carryover|$id".toByteArray()),
            carriedOver = true,
        )

    private fun GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow.toCarriedOverLine(): GroupDebtMovementLine =
        GroupDebtMovementLine(
            id =
                UUID.nameUUIDFromBytes(
                    "carryover|$payerId|$receiverId|${currency.uppercase()}|$month|${netAmount.asMoney()}".toByteArray(),
                ),
            payerId = payerId,
            receiverId = receiverId,
            month = month,
            transactionDate = null,
            currency = currency.uppercase(),
            deltaSigned = netAmount.asMoney(),
            reasonKind = GroupDebtMovementReasonKind.BENEFICIARY_CHARGE,
            createdByUserId = receiverId,
            carriedOver = true,
            projected = false,
            note = null,
            sourceWalletEventId = null,
            sourceWalletEvent = null,
            sourceMovementId = null,
            createdAt = null,
        )

    private fun deriveDebtMonth(
        eventDate: LocalDate,
        entries: List<WalletEntryEntity>,
    ): LocalDate =
        entries
            .asSequence()
            .filter { entry -> entry.walletItem?.type == WalletItemType.CREDIT_CARD }
            .mapNotNull { entry -> entry.bill?.billDate }
            .map { billDate -> billDate.withDayOfMonth(1) }
            .firstOrNull() ?: eventDate.withDayOfMonth(1)

    private suspend fun hydrateSourceWalletEvents(movements: List<GroupDebtMovementLine>): List<GroupDebtMovementLine> {
        val sourceWalletEventIds =
            movements
                .mapNotNull { it.sourceWalletEventId }
                .toSet()

        if (sourceWalletEventIds.isEmpty()) {
            return movements
        }

        val sourceWalletEventsById =
            walletEventListService
                .convertEntityToEntryListResponse(
                    walletEventRepository
                        .findAllByIdIn(sourceWalletEventIds)
                        .collectList()
                        .awaitSingle(),
                ).associateBy { it.id }

        return movements.map { movement ->
            val sourceWalletEvent =
                movement.sourceWalletEventId
                    ?.let { sourceWalletEventsById[it] }
            movement.copy(
                transactionDate = sourceWalletEvent?.date ?: movement.transactionDate,
                sourceWalletEvent = sourceWalletEvent ?: movement.sourceWalletEvent,
            )
        }
    }

    private data class MutablePosition(
        val userId: UUID,
        var remaining: BigDecimal,
    )

    private data class DebtRowKey(
        val payerId: UUID,
        val receiverId: UUID,
        val currency: String,
        val month: YearMonth,
    )

    private fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
