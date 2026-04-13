package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningGoalTrackResult
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningGroupContextResult
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulatedDebtRequest
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationRequest
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationResult
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationScopeType
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByWalletRow
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.simulation.PlanningSimulationContext
import com.ynixt.sharedfinances.domain.services.simulation.PlanningSimulationEngine
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class PlanningSimulationEngineImpl(
    private val walletItemRepository: WalletItemRepository,
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val groupUsersRepository: GroupUsersRepository,
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val creditCardBillService: CreditCardBillService,
    private val goalLedgerSummaryRepository: GoalLedgerCommittedSummaryRepository,
    private val dbClient: DatabaseClient,
    private val clock: Clock,
) : PlanningSimulationEngine {
    companion object {
        private const val DEFAULT_HORIZON_MONTHS = 6
        private const val MAX_HORIZON_MONTHS = 36
    }

    override suspend fun run(
        context: PlanningSimulationContext,
        request: PlanningSimulationRequest,
    ): PlanningSimulationResult {
        val today = LocalDate.now(clock)
        val startDate = request.startDate ?: today
        val horizonMonths = (request.horizonMonths ?: DEFAULT_HORIZON_MONTHS).coerceIn(1, MAX_HORIZON_MONTHS)
        val months = PlanningSimulationMath.buildMonthRange(YearMonth.from(startDate), horizonMonths)
        val maxDate = months.last().atEndOfMonth()
        val scopeData = resolveScopeData(context)

        val bankById = scopeData.bankAccounts.mapNotNull { bank -> bank.id?.let { it to bank } }.toMap()
        val bankIds = bankById.keys
        val openingByCurrency = aggregateOpeningByCurrency(scopeData.bankAccounts)

        val projectedByMonthCurrency =
            loadProjectedCashFlowByMonthCurrency(
                scopeData = scopeData,
                fromDate = startDate.plusDays(1),
                toDate = maxDate,
                inScopeBankIds = bankIds,
            )
        val creditCardBillOutflowByMonthCurrency =
            loadProjectedCreditCardBillOutflowByMonthCurrency(
                scopeData = scopeData,
                fromDate = startDate,
                toDate = maxDate,
            )
        val debtOutflowByMonthCurrency =
            buildDebtOutflowByMonthCurrency(
                debts = request.debts.orEmpty(),
                fromDate = startDate,
                toDate = maxDate,
                bankById = bankById,
                knownCurrencies = openingByCurrency.keys,
            )
        val scheduledGoalContributionsByMonthCurrency =
            loadScheduledGoalContributionsByMonthCurrency(
                scopeData = scopeData,
                fromDate = startDate,
                toDate = maxDate,
                inScopeBankIds = bankIds,
            )
        val committedByCurrency =
            loadCommittedAllocationsByCurrency(
                scopeData = scopeData,
                inScopeBankIds = bankIds,
            )

        val currencies =
            (
                openingByCurrency.keys +
                    projectedByMonthCurrency.keys.map { it.second } +
                    creditCardBillOutflowByMonthCurrency.keys.map { it.second } +
                    debtOutflowByMonthCurrency.keys.map { it.second } +
                    scheduledGoalContributionsByMonthCurrency.keys.map { it.second }
            ).toSortedSet()

        val baselineEvaluation =
            PlanningSimulationMath.evaluateTimeline(
                months = months,
                currencies = currencies,
                openingByCurrency = openingByCurrency,
                projectedByMonthCurrency = projectedByMonthCurrency,
                creditCardBillOutflowByMonthCurrency = creditCardBillOutflowByMonthCurrency,
                debtOutflowByMonthCurrency = debtOutflowByMonthCurrency,
                scheduledGoalContributionByMonthCurrency = scheduledGoalContributionsByMonthCurrency,
                openingBoostByCurrency = emptyMap(),
            )

        val counterfactualEvaluation =
            PlanningSimulationMath.evaluateTimeline(
                months = months,
                currencies = currencies,
                openingByCurrency = openingByCurrency,
                projectedByMonthCurrency = projectedByMonthCurrency,
                creditCardBillOutflowByMonthCurrency = creditCardBillOutflowByMonthCurrency,
                debtOutflowByMonthCurrency = debtOutflowByMonthCurrency,
                scheduledGoalContributionByMonthCurrency = scheduledGoalContributionsByMonthCurrency,
                openingBoostByCurrency = committedByCurrency,
            )

        val outcomeBand =
            PlanningSimulationMath.classify(
                baselineFits = baselineEvaluation.baselineFits,
                scheduledGoalTrackFits = baselineEvaluation.scheduledGoalTrackFits,
                counterfactualBaselineFits = counterfactualEvaluation.baselineFits,
            )

        return PlanningSimulationResult(
            scopeType = scopeData.scopeType,
            outcomeBand = outcomeBand,
            timeline = baselineEvaluation.timeline,
            goalTrack =
                PlanningGoalTrackResult(
                    canSustainScheduledContributions = baselineEvaluation.scheduledGoalTrackFits,
                    canSustainScheduledContributionsIfAllocationsAreFreed = counterfactualEvaluation.scheduledGoalTrackFits,
                    canFitIfAllocationsAreFreed = counterfactualEvaluation.baselineFits,
                    committedAllocationsByCurrency = committedByCurrency,
                ),
            groupContext =
                scopeData.groupContext?.let {
                    PlanningGroupContextResult(
                        incompleteSimulation = it.incompleteSimulation,
                        includedMembers = it.includedMembers,
                        excludedMembers = it.excludedMembers,
                        privacyLabels = it.privacyLabels,
                    )
                },
        )
    }

    private suspend fun resolveScopeData(context: PlanningSimulationContext): ScopeData =
        when {
            context.ownerUserId != null && context.ownerGroupId == null -> {
                val allItems =
                    walletItemRepository
                        .findAllByUserIdAndEnabled(
                            userId = context.ownerUserId,
                            enabled = true,
                            pageable = Pageable.unpaged(),
                        ).collectList()
                        .awaitSingle()

                ScopeData(
                    scopeType = PlanningSimulationScopeType.USER,
                    bankAccounts = allItems.filter { it.type == WalletItemType.BANK_ACCOUNT },
                    creditCards = allItems.filter { it.type == WalletItemType.CREDIT_CARD },
                    userIdsForProjection = setOf(context.ownerUserId),
                    groupIdForProjection = null,
                    groupContext = null,
                )
            }

            context.ownerGroupId != null && context.ownerUserId == null -> {
                val members = groupUsersRepository.findAllMembers(context.ownerGroupId).collectList().awaitSingle()
                val includedMemberIds = members.filter { it.allowPlanningSimulator }.map { it.userId }.toSet()
                val excludedMembers = members.count { !it.allowPlanningSimulator }

                val bankAccounts =
                    groupWalletItemRepository
                        .findAllAssociatedToGroup(context.ownerGroupId, WalletItemType.BANK_ACCOUNT)
                        .collectList()
                        .awaitSingle()
                        .filter { it.enabled && includedMemberIds.contains(it.userId) }
                val creditCards =
                    groupWalletItemRepository
                        .findAllAssociatedToGroup(context.ownerGroupId, WalletItemType.CREDIT_CARD)
                        .collectList()
                        .awaitSingle()
                        .filter { it.enabled && includedMemberIds.contains(it.userId) }

                val requesterIncluded = includedMemberIds.contains(context.requestedByUserId)
                val opaqueOtherMembersCount =
                    if (requesterIncluded) {
                        (includedMemberIds.size - 1).coerceAtLeast(0)
                    } else {
                        includedMemberIds.size
                    }

                ScopeData(
                    scopeType = PlanningSimulationScopeType.GROUP,
                    bankAccounts = bankAccounts,
                    creditCards = creditCards,
                    userIdsForProjection = includedMemberIds,
                    groupIdForProjection = context.ownerGroupId,
                    groupContext =
                        GroupContext(
                            incompleteSimulation = excludedMembers > 0,
                            includedMembers = includedMemberIds.size,
                            excludedMembers = excludedMembers,
                            privacyLabels = (1..opaqueOtherMembersCount).map { idx -> "Personal finances (member $idx)" },
                        ),
                )
            }

            else -> error("Invalid planning simulation context scope")
        }

    private fun aggregateOpeningByCurrency(items: List<WalletItemEntity>): Map<String, BigDecimal> =
        items
            .groupBy { it.currency.uppercase() }
            .mapValues { (_, entries) ->
                entries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.balance }.asMoney()
            }

    private suspend fun loadProjectedCashFlowByMonthCurrency(
        scopeData: ScopeData,
        fromDate: LocalDate,
        toDate: LocalDate,
        inScopeBankIds: Set<UUID>,
    ): Map<Pair<YearMonth, String>, BigDecimal> {
        if (inScopeBankIds.isEmpty() || fromDate.isAfter(toDate)) {
            return emptyMap()
        }

        val projected = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
        val simulatedEvents = mutableListOf<com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse>()

        scopeData.userIdsForProjection.forEach { userId ->
            simulatedEvents +=
                recurrenceSimulationService.simulateGeneration(
                    minimumEndExecution = fromDate,
                    maximumNextExecution = toDate,
                    userId = userId,
                    groupId = null,
                    walletItemId = null,
                    billDate = null,
                )
        }

        scopeData.groupIdForProjection?.let { groupId ->
            simulatedEvents +=
                recurrenceSimulationService.simulateGeneration(
                    minimumEndExecution = fromDate,
                    maximumNextExecution = toDate,
                    userId = null,
                    groupId = groupId,
                    walletItemId = null,
                    billDate = null,
                )
        }

        simulatedEvents.forEach { event ->
            val month = YearMonth.from(event.date)
            event.entries.forEach { entry ->
                if (!inScopeBankIds.contains(entry.walletItemId)) {
                    return@forEach
                }

                val currency = entry.walletItem.currency.uppercase()
                val key = month to currency
                projected[key] = projected.getOrDefault(key, BigDecimal.ZERO).add(entry.value).asMoney()
            }
        }

        return projected
    }

    private suspend fun loadProjectedCreditCardBillOutflowByMonthCurrency(
        scopeData: ScopeData,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): Map<Pair<YearMonth, String>, BigDecimal> {
        val cardsById =
            scopeData.creditCards
                .mapNotNull { card -> card.id?.let { it to card } }
                .toMap()

        if (cardsById.isEmpty() || fromDate.isAfter(toDate)) {
            return emptyMap()
        }

        val map = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
        scopeData.userIdsForProjection.forEach { userId ->
            val bills =
                creditCardBillService.findAllOpenByDueDateBetween(
                    userId = userId,
                    minimumDueDate = fromDate,
                    maximumDueDate = toDate,
                )

            bills.forEach { bill ->
                val card = cardsById[bill.creditCardId] ?: return@forEach
                val outflow =
                    bill.value
                        .negate()
                        .max(BigDecimal.ZERO)
                        .asMoney()
                if (outflow.compareTo(BigDecimal.ZERO) == 0) {
                    return@forEach
                }
                val key = YearMonth.from(bill.dueDate) to card.currency.uppercase()
                map[key] = map.getOrDefault(key, BigDecimal.ZERO).add(outflow).asMoney()
            }
        }

        return map
    }

    private fun buildDebtOutflowByMonthCurrency(
        debts: List<PlanningSimulatedDebtRequest>,
        fromDate: LocalDate,
        toDate: LocalDate,
        bankById: Map<UUID, WalletItemEntity>,
        knownCurrencies: Set<String>,
    ): Map<Pair<YearMonth, String>, BigDecimal> {
        if (debts.isEmpty() || fromDate.isAfter(toDate)) {
            return emptyMap()
        }

        val byMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()

        debts.forEach { debt ->
            val total = debt.amount.abs().asMoney()
            if (total.compareTo(BigDecimal.ZERO) == 0) {
                return@forEach
            }

            val currency = resolveDebtCurrency(debt, bankById, knownCurrencies)
            val firstPaymentDate = debt.firstPaymentDate ?: fromDate
            val installments = (debt.installments ?: 1).coerceAtLeast(1)
            val installmentsPlan = PlanningSimulationMath.splitInstallments(total, installments)

            installmentsPlan.forEachIndexed { idx, installmentValue ->
                val dueDate = firstPaymentDate.plusMonths(idx.toLong())
                if (dueDate.isBefore(fromDate)) {
                    return@forEachIndexed
                }
                if (dueDate.isAfter(toDate)) {
                    return@forEachIndexed
                }

                val key = YearMonth.from(dueDate) to currency
                byMonthCurrency[key] = byMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(installmentValue).asMoney()
            }
        }

        return byMonthCurrency
    }

    private suspend fun loadScheduledGoalContributionsByMonthCurrency(
        scopeData: ScopeData,
        fromDate: LocalDate,
        toDate: LocalDate,
        inScopeBankIds: Set<UUID>,
    ): Map<Pair<YearMonth, String>, BigDecimal> {
        if (fromDate.isAfter(toDate) || inScopeBankIds.isEmpty()) {
            return emptyMap()
        }

        val schedules =
            when (scopeData.scopeType) {
                PlanningSimulationScopeType.USER -> loadSchedulesForUser(scopeData.userIdsForProjection.first(), fromDate, toDate)
                PlanningSimulationScopeType.GROUP ->
                    loadSchedulesForGroup(
                        groupId = requireNotNull(scopeData.groupIdForProjection),
                        allowedUserIds = scopeData.userIdsForProjection,
                        fromDate = fromDate,
                        toDate = toDate,
                    )
            }.filter { schedule -> inScopeBankIds.contains(schedule.walletItemId) }

        val byMonthCurrency = linkedMapOf<Pair<YearMonth, String>, BigDecimal>()
        schedules.forEach { schedule ->
            expandScheduleOccurrences(schedule, fromDate, toDate).forEach { occurrenceDate ->
                val key = YearMonth.from(occurrenceDate) to schedule.currency.uppercase()
                byMonthCurrency[key] = byMonthCurrency.getOrDefault(key, BigDecimal.ZERO).add(schedule.amount).asMoney()
            }
        }

        return byMonthCurrency
    }

    private suspend fun loadCommittedAllocationsByCurrency(
        scopeData: ScopeData,
        inScopeBankIds: Set<UUID>,
    ): Map<String, BigDecimal> {
        if (inScopeBankIds.isEmpty()) {
            return emptyMap()
        }

        val rows: List<GoalCommittedByWalletRow> =
            when (scopeData.scopeType) {
                PlanningSimulationScopeType.USER ->
                    goalLedgerSummaryRepository
                        .summarizeCommittedByUserGoals(scopeData.userIdsForProjection.first())
                        .collectList()
                        .awaitSingle()
                        .filter { inScopeBankIds.contains(it.walletItemId) }

                PlanningSimulationScopeType.GROUP ->
                    summarizeCommittedByGroupGoals(
                        groupId = requireNotNull(scopeData.groupIdForProjection),
                        allowedUserIds = scopeData.userIdsForProjection,
                    ).filter { inScopeBankIds.contains(it.walletItemId) }
            }

        return rows
            .groupBy { it.currency.uppercase() }
            .mapValues { (_, values) ->
                values
                    .fold(BigDecimal.ZERO) { acc, row -> acc + row.committed }
                    .max(BigDecimal.ZERO)
                    .asMoney()
            }
    }

    private suspend fun loadSchedulesForUser(
        userId: UUID,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<GoalScheduleRow> {
        val sql =
            """
            SELECT
                s.id,
                s.wallet_item_id,
                wi.user_id AS wallet_owner_user_id,
                s.amount,
                s.currency,
                s.periodicity,
                s.qty_executed,
                s.qty_limit,
                s.next_execution,
                s.end_execution
            FROM financial_goal_contribution_schedule s
            INNER JOIN financial_goal g ON g.id = s.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = s.wallet_item_id
            WHERE
                wi.type = 'BANK_ACCOUNT'
                AND s.removes_allocation = FALSE
                AND s.next_execution IS NOT NULL
                AND s.next_execution <= :toDate
                AND (s.end_execution IS NULL OR s.end_execution >= :fromDate)
                AND (
                    g.user_id = :userId
                    OR g.group_id IN (SELECT gu.group_id FROM group_user gu WHERE gu.user_id = :userId)
                )
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("fromDate", fromDate)
            .bind("toDate", toDate)
            .map { row, _ -> row.toGoalScheduleRow() }
            .all()
            .collectList()
            .awaitSingle()
    }

    private suspend fun loadSchedulesForGroup(
        groupId: UUID,
        allowedUserIds: Set<UUID>,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<GoalScheduleRow> {
        if (allowedUserIds.isEmpty()) {
            return emptyList()
        }

        val sql =
            """
            SELECT
                s.id,
                s.wallet_item_id,
                wi.user_id AS wallet_owner_user_id,
                s.amount,
                s.currency,
                s.periodicity,
                s.qty_executed,
                s.qty_limit,
                s.next_execution,
                s.end_execution
            FROM financial_goal_contribution_schedule s
            INNER JOIN financial_goal g ON g.id = s.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = s.wallet_item_id
            WHERE
                wi.type = 'BANK_ACCOUNT'
                AND s.removes_allocation = FALSE
                AND s.next_execution IS NOT NULL
                AND s.next_execution <= :toDate
                AND (s.end_execution IS NULL OR s.end_execution >= :fromDate)
                AND g.group_id = :groupId
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("fromDate", fromDate)
            .bind("toDate", toDate)
            .map { row, _ -> row.toGoalScheduleRow() }
            .all()
            .collectList()
            .awaitSingle()
            .filter { row -> allowedUserIds.contains(row.walletOwnerUserId) }
    }

    private suspend fun summarizeCommittedByGroupGoals(
        groupId: UUID,
        allowedUserIds: Set<UUID>,
    ): List<GoalCommittedByWalletRow> {
        if (allowedUserIds.isEmpty()) {
            return emptyList()
        }

        val sql =
            """
            SELECT
                m.wallet_item_id,
                wi.user_id AS wallet_owner_user_id,
                wi.currency,
                SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN financial_goal g ON g.id = m.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE
                wi.type = 'BANK_ACCOUNT'
                AND g.group_id = :groupId
            GROUP BY m.wallet_item_id, wi.user_id, wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                row.get("wallet_owner_user_id", UUID::class.java)!! to
                    GoalCommittedByWalletRow(
                        walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                        currency = row.get("currency", String::class.java)!!,
                        committed = row.get("committed", BigDecimal::class.java)!!,
                    )
            }.all()
            .collectList()
            .awaitSingle()
            .filter { (ownerUserId, _) -> allowedUserIds.contains(ownerUserId) }
            .map { (_, row) -> row }
    }

    private fun expandScheduleOccurrences(
        schedule: GoalScheduleRow,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<LocalDate> {
        val start = schedule.nextExecution ?: return emptyList()
        var cursor = start
        var qtyExecuted = schedule.qtyExecuted
        val result = mutableListOf<LocalDate>()

        while (!cursor.isAfter(toDate)) {
            if (schedule.endExecution != null && cursor.isAfter(schedule.endExecution)) {
                break
            }

            if (!cursor.isBefore(fromDate)) {
                result += cursor
            }

            qtyExecuted += 1
            if (schedule.qtyLimit != null && qtyExecuted >= schedule.qtyLimit) {
                break
            }

            if (schedule.periodicity == RecurrenceType.SINGLE) {
                break
            }

            cursor =
                when (schedule.periodicity) {
                    RecurrenceType.DAILY -> cursor.plusDays(1)
                    RecurrenceType.WEEKLY -> cursor.plusWeeks(1)
                    RecurrenceType.MONTHLY -> cursor.plusMonths(1)
                    RecurrenceType.YEARLY -> cursor.plusYears(1)
                    RecurrenceType.SINGLE -> cursor
                }
        }

        return result
    }

    private fun resolveDebtCurrency(
        debt: PlanningSimulatedDebtRequest,
        bankById: Map<UUID, WalletItemEntity>,
        knownCurrencies: Set<String>,
    ): String =
        when {
            debt.sourceWalletItemId != null -> bankById[debt.sourceWalletItemId]?.currency?.uppercase()
            !debt.currency.isNullOrBlank() -> debt.currency!!.uppercase()
            knownCurrencies.size == 1 -> knownCurrencies.first()
            knownCurrencies.isNotEmpty() -> knownCurrencies.first()
            else -> "USD"
        } ?: error("Debt source wallet item was not found in planning scope: ${debt.sourceWalletItemId}")

    private fun BigDecimal.asMoney(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

    private data class ScopeData(
        val scopeType: PlanningSimulationScopeType,
        val bankAccounts: List<WalletItemEntity>,
        val creditCards: List<WalletItemEntity>,
        val userIdsForProjection: Set<UUID>,
        val groupIdForProjection: UUID?,
        val groupContext: GroupContext?,
    )

    private data class GroupContext(
        val incompleteSimulation: Boolean,
        val includedMembers: Int,
        val excludedMembers: Int,
        val privacyLabels: List<String>,
    )

    private data class GoalScheduleRow(
        val id: UUID,
        val walletItemId: UUID,
        val walletOwnerUserId: UUID,
        val amount: BigDecimal,
        val currency: String,
        val periodicity: RecurrenceType,
        val qtyExecuted: Int,
        val qtyLimit: Int?,
        val nextExecution: LocalDate?,
        val endExecution: LocalDate?,
    )

    private fun io.r2dbc.spi.Readable.toGoalScheduleRow(): GoalScheduleRow =
        GoalScheduleRow(
            id = get("id", UUID::class.java)!!,
            walletItemId = get("wallet_item_id", UUID::class.java)!!,
            walletOwnerUserId = get("wallet_owner_user_id", UUID::class.java)!!,
            amount = get("amount", BigDecimal::class.java)!!.asMoney(),
            currency = get("currency", String::class.java)!!,
            periodicity = RecurrenceType.valueOf(get("periodicity", String::class.java)!!),
            qtyExecuted = get("qty_executed", Integer::class.java)?.toInt() ?: 0,
            qtyLimit = get("qty_limit", Integer::class.java)?.toInt(),
            nextExecution = get("next_execution", LocalDate::class.java),
            endExecution = get("end_execution", LocalDate::class.java),
        )
}
