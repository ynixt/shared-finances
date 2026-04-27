package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtMovementNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupDebtAdjustmentException
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyComposition
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtPairBalance
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

        val currentMonth = YearMonth.from(LocalDate.now(clock))
        val rowsForSelectedMonth =
            debtDatabaseClientRepository
                .listMonthlyComposition(groupId)
                .collectList()
                .awaitSingle()
                .filter { row -> !row.month.isAfter(selectedMonth) }
                .toMutableList()

        if (!selectedMonth.isBefore(currentMonth)) {
            rowsForSelectedMonth +=
                loadProjectedMonthlyRows(
                    userId = userId,
                    groupId = groupId,
                    selectedMonth = selectedMonth,
                )
        }

        return GroupDebtWorkspace(
            balances = mapBalances(rowsForSelectedMonth),
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
                ).collectList()
                .awaitSingle()
                .map { row -> row.toLine() }

        return hydrateSourceWalletEvents(history)
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
        selectedMonth: YearMonth,
    ): List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow> {
        val today = LocalDate.now(clock)
        val selectedMonthEnd = selectedMonth.atEndOfMonth()
        if (selectedMonthEnd.isBefore(today)) {
            return emptyList()
        }

        val projectedByPairAndCurrency = mutableMapOf<Triple<UUID, UUID, String>, BigDecimal>()
        recurrenceSimulationService
            .simulateGenerationWithFilters(
                minimumEndExecution = today,
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
            ).asSequence()
            .filter { event -> YearMonth.from(event.date) == selectedMonth }
            .forEach { event ->
                deriveProjectedDebtMovementsFromEvent(event).forEach { projected ->
                    if (projected.amount.compareTo(ZERO) <= 0) {
                        return@forEach
                    }

                    val key = Triple(projected.payerId, projected.receiverId, projected.currency.uppercase())
                    projectedByPairAndCurrency[key] =
                        projectedByPairAndCurrency
                            .getOrDefault(key, ZERO)
                            .add(projected.amount)
                            .asMoney()
                }
            }

        return projectedByPairAndCurrency.entries
            .asSequence()
            .filter { (_, amount) -> amount.compareTo(ZERO) > 0 }
            .map { (key, amount) ->
                GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow(
                    payerId = key.first,
                    receiverId = key.second,
                    currency = key.third,
                    month = selectedMonth,
                    netAmount = amount.asMoney(),
                    chargeDelta = amount.asMoney(),
                    settlementDelta = ZERO,
                    manualAdjustmentDelta = ZERO,
                )
            }.toList()
    }

    private fun mapBalances(rows: List<GroupMemberDebtDatabaseClientRepository.DebtMonthlyCompositionRow>): List<GroupDebtPairBalance> =
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
            }.sortedWith(
                compareBy<GroupDebtPairBalance> { it.payerId.toString() }
                    .thenBy { it.receiverId.toString() }
                    .thenBy { it.currency },
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

    private fun deriveMovements(
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
                listOf(deriveSettlementMovement(actorUserId, groupId, event, entries))

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

    private fun deriveSettlementMovement(
        actorUserId: UUID,
        groupId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    ): GroupMemberDebtMovementEntity {
        val origin = entries.firstOrNull { it.value < ZERO } ?: entries.first()
        val target = entries.firstOrNull { it.walletItemId != origin.walletItemId } ?: entries.last()
        val originOwner = requireNotNull(origin.walletItem?.userId) { "Settlement origin wallet owner must be hydrated" }
        val targetOwner = requireNotNull(target.walletItem?.userId) { "Settlement target wallet owner must be hydrated" }

        return GroupMemberDebtMovementEntity(
            groupId = groupId,
            payerId = originOwner,
            receiverId = targetOwner,
            month = event.date.withDayOfMonth(1),
            currency = requireNotNull(origin.walletItem?.currency).uppercase(),
            deltaSigned = origin.value.asMoney(),
            reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
            createdByUserId = actorUserId,
            sourceWalletEventId = event.id,
        )
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
        val month = event.date.withDayOfMonth(1)
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
            currency = currency.uppercase(),
            deltaSigned = deltaSigned.asMoney(),
            reasonKind = reasonKind,
            createdByUserId = createdByUserId,
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
            currency = currency.uppercase(),
            deltaSigned = deltaSigned.asMoney(),
            reasonKind = GroupDebtMovementReasonKind.valueOf(reasonKind),
            createdByUserId = createdByUserId,
            note = note,
            sourceWalletEventId = sourceWalletEventId,
            sourceMovementId = sourceMovementId,
            createdAt = createdAt,
        )

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
            movement.copy(
                sourceWalletEvent =
                    movement.sourceWalletEventId
                        ?.let { sourceWalletEventsById[it] },
            )
        }
    }

    private data class MutablePosition(
        val userId: UUID,
        var remaining: BigDecimal,
    )

    private fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
