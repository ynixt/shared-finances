package com.ynixt.sharedfinances.resources.services.goals

import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalContributionScheduleEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalLedgerMovementEntity
import com.ynixt.sharedfinances.domain.entities.goals.FinancialGoalTargetEntity
import com.ynixt.sharedfinances.domain.enums.GoalLedgerMovementKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalInvalidWalletException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalLedgerMovementNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalScheduleNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.FinancialGoalUnsupportedCurrencyException
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalDetail
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalHeader
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalTargetAmount
import com.ynixt.sharedfinances.domain.models.goals.GoalCommitmentChartSeries
import com.ynixt.sharedfinances.domain.models.goals.GoalCommitmentMonthlyPoint
import com.ynixt.sharedfinances.domain.models.goals.GoalContributionScheduleLine
import com.ynixt.sharedfinances.domain.models.goals.GoalLedgerMovementLine
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.goals.EditFinancialGoalInput
import com.ynixt.sharedfinances.domain.services.goals.FinancialGoalManagementService
import com.ynixt.sharedfinances.domain.services.goals.NewFinancialGoalInput
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalContributionScheduleSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalLedgerMovementSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalTargetSpringDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

@Service
class FinancialGoalManagementServiceImpl(
    private val financialGoalRepository: FinancialGoalSpringDataRepository,
    private val financialGoalTargetRepository: FinancialGoalTargetSpringDataRepository,
    private val ledgerMovementRepository: FinancialGoalLedgerMovementSpringDataRepository,
    private val scheduleRepository: FinancialGoalContributionScheduleSpringDataRepository,
    private val ledgerSummaryRepository: GoalLedgerCommittedSummaryRepository,
    private val walletItemRepository: WalletItemRepository,
    private val walletItemService: WalletItemService,
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val groupRepository: GroupRepository,
    private val groupPermissionService: GroupPermissionService,
    private val recurrenceService: RecurrenceService,
    private val clock: Clock,
) : FinancialGoalManagementService {
    companion object {
        private const val COMMITMENT_CHART_MIN_MONTHS = 5
    }

    override suspend fun listIndividualGoals(
        userId: UUID,
        pageable: Pageable,
    ): Page<FinancialGoalHeader> =
        createPage(
            pageable = pageable,
            countFn = { financialGoalRepository.countByUserId(userId) },
        ) {
            financialGoalRepository
                .findAllByUserIdOrderByNameAscIdAsc(userId, pageable)
                .map(::toHeader)
        }

    override suspend fun listGroupGoals(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<FinancialGoalHeader> {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = groupId)) {
            throw FinancialGoalForbiddenException()
        }
        return createPage(
            pageable = pageable,
            countFn = { financialGoalRepository.countByGroupId(groupId) },
        ) {
            financialGoalRepository
                .findAllByGroupIdOrderByNameAscIdAsc(groupId, pageable)
                .map(::toHeader)
        }
    }

    override suspend fun getGoalDetail(
        userId: UUID,
        goalId: UUID,
    ): FinancialGoalDetail {
        val goalEntity = loadGoal(goalId)
        ensureViewAccess(userId, goalEntity)

        val targets =
            financialGoalTargetRepository
                .findAllByFinancialGoalId(goalId)
                .map { FinancialGoalTargetAmount(currency = it.currency, targetAmount = it.targetAmount.asMoney()) }
                .collectList()
                .awaitSingle()

        val movementEntitiesAscAll =
            ledgerMovementRepository
                .findAllByFinancialGoalIdOrderByMovementDateAscIdAsc(goalId)
                .collectList()
                .awaitSingle()

        val walletIds = movementEntitiesAscAll.map { it.walletItemId }.toSet()

        val walletInfoById: Map<UUID, WalletInfo> =
            if (walletIds.isEmpty()) {
                emptyMap()
            } else {
                walletItemService
                    .findAllByIdIn(walletIds)
                    .toList()
                    .associate { wi ->
                        val id = requireNotNull(wi.id) { "wallet item id" }
                        id to
                            WalletInfo(
                                currency = wi.currency.uppercase(Locale.ROOT),
                                name = wi.name,
                            )
                    }
            }

        val committedByCurrency =
            ledgerSummaryRepository
                .summarizeCommittedByGoal(goalId)
                .collectList()
                .awaitSingle()
                .associate { row ->
                    row.currency.uppercase() to row.committed.asMoney()
                }

        val commitmentChart =
            buildCommitmentChart(
                goalEntity = goalEntity,
                targets = targets,
                movementsAsc = movementEntitiesAscAll,
                walletInfoById = walletInfoById,
            )

        return FinancialGoalDetail(
            goal = toHeader(goalEntity),
            targets = targets,
            committedByCurrency = committedByCurrency,
            commitmentChart = commitmentChart,
        )
    }

    @Transactional
    override suspend fun createGoal(
        userId: UUID,
        input: NewFinancialGoalInput,
    ): FinancialGoalHeader {
        require(input.targets.isNotEmpty()) { "At least one currency target is required" }
        val savedGoal =
            financialGoalRepository
                .save(
                    FinancialGoalEntity(
                        name = input.name.trim(),
                        description = input.description?.trim()?.ifBlank { null },
                        userId = userId,
                        groupId = null,
                        deadline = input.deadline,
                    ),
                ).awaitSingle()

        val goalId = savedGoal.id!!

        financialGoalTargetRepository
            .saveAll(
                input.targets.map { t ->
                    FinancialGoalTargetEntity(
                        financialGoalId = goalId,
                        currency = t.currency.uppercase(),
                        targetAmount = t.targetAmount.asMoney(),
                    )
                },
            ).collectList()
            .awaitSingle()

        return toHeader(savedGoal)
    }

    @Transactional
    override suspend fun createGoalForGroup(
        userId: UUID,
        groupId: UUID,
        input: NewFinancialGoalInput,
    ): FinancialGoalHeader {
        require(input.targets.isNotEmpty()) { "At least one currency target is required" }
        if (!groupPermissionService.hasPermission(userId, groupId, GroupPermissions.MANAGE_GOALS)) {
            throw FinancialGoalForbiddenException()
        }

        val savedGoal =
            financialGoalRepository
                .save(
                    FinancialGoalEntity(
                        name = input.name.trim(),
                        description = input.description?.trim()?.ifBlank { null },
                        userId = null,
                        groupId = groupId,
                        deadline = input.deadline,
                    ),
                ).awaitSingle()

        val goalId = savedGoal.id!!

        financialGoalTargetRepository
            .saveAll(
                input.targets.map { t ->
                    FinancialGoalTargetEntity(
                        financialGoalId = goalId,
                        currency = t.currency.uppercase(),
                        targetAmount = t.targetAmount.asMoney(),
                    )
                },
            ).collectList()
            .awaitSingle()

        return toHeader(savedGoal)
    }

    @Transactional
    override suspend fun updateGoal(
        userId: UUID,
        goalId: UUID,
        input: EditFinancialGoalInput,
    ): FinancialGoalHeader {
        require(input.targets.isNotEmpty()) { "At least one currency target is required" }
        val existing = loadGoal(goalId)
        ensureEditAccess(userId, existing)

        financialGoalRepository
            .save(
                FinancialGoalEntity(
                    name = input.name.trim(),
                    description = input.description?.trim()?.ifBlank { null },
                    userId = existing.userId,
                    groupId = existing.groupId,
                    deadline = input.deadline,
                ).also {
                    it.id = existing.id
                    it.createdAt = existing.createdAt
                    it.updatedAt = OffsetDateTime.now(clock)
                },
            ).awaitSingle()

        financialGoalTargetRepository.deleteAllByFinancialGoalId(goalId).awaitSingle()
        financialGoalTargetRepository
            .saveAll(
                input.targets.map { t ->
                    FinancialGoalTargetEntity(
                        financialGoalId = goalId,
                        currency = t.currency.uppercase(),
                        targetAmount = t.targetAmount.asMoney(),
                    )
                },
            ).collectList()
            .awaitSingle()

        return toHeader(loadGoal(goalId))
    }

    @Transactional
    override suspend fun deleteGoal(
        userId: UUID,
        goalId: UUID,
    ) {
        val existing = loadGoal(goalId)
        ensureEditAccess(userId, existing)
        when {
            existing.userId != null -> {
                val deleted = financialGoalRepository.deleteByIdAndUserId(goalId, userId).awaitSingle()
                if (deleted == 0L) {
                    throw FinancialGoalNotFoundException(goalId)
                }
            }
            existing.groupId != null -> {
                financialGoalRepository.deleteById(goalId.toString()).then().awaitSingle()
            }
        }
    }

    @Transactional
    override suspend fun allocateImmediate(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        allocationDate: LocalDate,
        note: String?,
    ) {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val wallet = resolveBankWallet(goal, walletItemId)
        ensureWalletCurrencyIsTargeted(goalId = goal.id!!, walletCurrency = wallet.currency)
        val signed = amount.asMoney()
        if (signed.compareTo(BigDecimal.ZERO) <= 0) {
            throw FinancialGoalInvalidWalletException(walletItemId)
        }
        persistMovement(
            goalId = goalId,
            walletItemId = wallet.id!!,
            signedAmount = signed,
            note = note?.trim()?.ifBlank { null },
            kind = GoalLedgerMovementKind.IMMEDIATE,
            scheduleId = null,
            movementDate = allocationDate,
        )
    }

    @Transactional
    override suspend fun reverseAllocation(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        note: String?,
    ) {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val wallet = resolveBankWallet(goal, walletItemId)
        ensureWalletCurrencyIsTargeted(goalId = goal.id!!, walletCurrency = wallet.currency)
        val removal = amount.asMoney()
        if (removal.compareTo(BigDecimal.ZERO) <= 0) {
            throw FinancialGoalInvalidWalletException(walletItemId)
        }
        persistMovement(
            goalId = goalId,
            walletItemId = wallet.id!!,
            signedAmount = removal.negate().asMoney(),
            note = note?.trim()?.ifBlank { null },
            kind = GoalLedgerMovementKind.IMMEDIATE,
            scheduleId = null,
            movementDate = LocalDate.now(clock),
        )
    }

    override suspend fun getLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
    ): GoalLedgerMovementLine {
        val goal = loadGoal(goalId)
        ensureViewAccess(userId, goal)
        val entity =
            ledgerMovementRepository
                .findByIdAndFinancialGoalId(movementId, goalId)
                .awaitSingleOrNull() ?: throw FinancialGoalLedgerMovementNotFoundException(movementId)
        val wallet = walletItemService.findOne(entity.walletItemId)
        val currency = wallet?.currency?.uppercase(Locale.ROOT) ?: "BRL"
        val name = wallet?.name ?: entity.walletItemId.toString()
        return toMovementLine(entity, currency = currency, walletItemName = name)
    }

    override suspend fun listLedgerMovements(
        userId: UUID,
        goalId: UUID,
        pageable: Pageable,
    ): Page<GoalLedgerMovementLine> {
        val goal = loadGoal(goalId)
        ensureViewAccess(userId, goal)
        val count = ledgerMovementRepository.countByFinancialGoalId(goalId).awaitSingle()
        if (count == 0L) {
            return org.springframework.data.domain.Page
                .empty(pageable)
        }

        val entities =
            ledgerMovementRepository
                .findAllByFinancialGoalIdOrderByMovementDateDescIdDesc(goalId, pageable)
                .collectList()
                .awaitSingle()

        val walletIds = entities.map { it.walletItemId }.toSet()
        val walletInfoById =
            if (walletIds.isEmpty()) {
                emptyMap()
            } else {
                walletItemService
                    .findAllByIdIn(walletIds)
                    .toList()
                    .associate { wi ->
                        val id = requireNotNull(wi.id) { "wallet item id" }
                        id to WalletInfo(currency = wi.currency.uppercase(Locale.ROOT), name = wi.name)
                    }
            }

        val lines =
            entities.map { entity ->
                val w = walletInfoById[entity.walletItemId]
                toMovementLine(
                    entity = entity,
                    currency = w?.currency ?: "BRL",
                    walletItemName = w?.name ?: entity.walletItemId.toString(),
                )
            }

        return org.springframework.data.domain
            .PageImpl(lines, pageable, count)
    }

    @Transactional
    override suspend fun editLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
        newSignedAmount: BigDecimal,
        allocationDate: LocalDate?,
        note: String?,
    ) {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val existing =
            ledgerMovementRepository
                .findByIdAndFinancialGoalId(movementId, goalId)
                .awaitSingleOrNull() ?: throw FinancialGoalLedgerMovementNotFoundException(movementId)

        val newMovementDate = allocationDate ?: existing.movementDate
        val newNote =
            if (note == null) {
                existing.note
            } else {
                note.trim().ifBlank { null }
            }
        val updated =
            FinancialGoalLedgerMovementEntity(
                financialGoalId = existing.financialGoalId,
                walletItemId = existing.walletItemId,
                signedAmount = newSignedAmount.asMoney(),
                note = newNote,
                movementKind = existing.movementKind,
                scheduleId = existing.scheduleId,
                movementDate = newMovementDate,
            ).also {
                it.id = existing.id
                it.createdAt = existing.createdAt
                it.updatedAt = OffsetDateTime.now(clock)
            }
        ledgerMovementRepository.save(updated).awaitSingle()
    }

    @Transactional
    override suspend fun deleteLedgerMovement(
        userId: UUID,
        goalId: UUID,
        movementId: UUID,
    ) {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)

        val existing =
            ledgerMovementRepository
                .findByIdAndFinancialGoalId(movementId, goalId)
                .awaitSingleOrNull() ?: throw FinancialGoalLedgerMovementNotFoundException(movementId)

        ledgerMovementRepository.deleteById(existing.id!!.toString()).awaitSingleOrNull()
    }

    @Transactional
    override suspend fun createSchedule(
        userId: UUID,
        goalId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        periodicity: RecurrenceType,
        firstExecution: LocalDate,
        qtyLimit: Int?,
        removesAllocation: Boolean,
    ): GoalContributionScheduleLine {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val wallet = resolveBankWallet(goal, walletItemId)
        ensureWalletCurrencyIsTargeted(goalId = goal.id!!, walletCurrency = wallet.currency)
        val amt = amount.asMoney()
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw FinancialGoalInvalidWalletException(walletItemId)
        }
        val endExecution =
            recurrenceService.calculateEndDate(
                lastExecution = firstExecution,
                periodicity = periodicity,
                qtyExecuted = 0,
                qtyLimit = qtyLimit,
            )
        val saved =
            scheduleRepository
                .save(
                    FinancialGoalContributionScheduleEntity(
                        financialGoalId = goalId,
                        walletItemId = wallet.id!!,
                        amount = amt,
                        currency = wallet.currency.uppercase(),
                        periodicity = periodicity,
                        qtyExecuted = 0,
                        qtyLimit = qtyLimit,
                        lastExecution = null,
                        nextExecution = firstExecution,
                        endExecution = endExecution,
                        removesAllocation = removesAllocation,
                    ),
                ).awaitSingle()
        return toScheduleLine(
            saved,
            mapOf(
                wallet.id!! to
                    WalletInfo(
                        currency = wallet.currency.uppercase(Locale.ROOT),
                        name = wallet.name,
                    ),
            ),
        )
    }

    override suspend fun getSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
    ): GoalContributionScheduleLine {
        val goal = loadGoal(goalId)
        ensureViewAccess(userId, goal)
        val entity =
            scheduleRepository
                .findByIdAndFinancialGoalId(scheduleId, goalId)
                .awaitSingleOrNull() ?: throw FinancialGoalScheduleNotFoundException(scheduleId)
        val wallet = walletItemService.findOne(entity.walletItemId)
        val currency = wallet?.currency?.uppercase(Locale.ROOT) ?: "BRL"
        val name = wallet?.name ?: entity.walletItemId.toString()
        return toScheduleLine(
            entity,
            mapOf(entity.walletItemId to WalletInfo(currency = currency, name = name)),
        )
    }

    override suspend fun listSchedules(
        userId: UUID,
        goalId: UUID,
        pageable: Pageable,
    ): Page<GoalContributionScheduleLine> {
        val goal = loadGoal(goalId)
        ensureViewAccess(userId, goal)

        val count = scheduleRepository.countByFinancialGoalId(goalId).awaitSingle()
        if (count == 0L) {
            return org.springframework.data.domain.Page
                .empty(pageable)
        }

        val entities =
            scheduleRepository
                .findAllByFinancialGoalIdOrderByNextExecutionAscIdAsc(goalId, pageable)
                .collectList()
                .awaitSingle()

        val walletIds = entities.map { it.walletItemId }.toSet()
        val walletInfoById =
            if (walletIds.isEmpty()) {
                emptyMap()
            } else {
                walletItemService
                    .findAllByIdIn(walletIds)
                    .toList()
                    .associate { wi ->
                        val id = requireNotNull(wi.id) { "wallet item id" }
                        id to WalletInfo(currency = wi.currency.uppercase(Locale.ROOT), name = wi.name)
                    }
            }

        val lines = entities.map { entity -> toScheduleLine(entity, walletInfoById) }
        return org.springframework.data.domain
            .PageImpl(lines, pageable, count)
    }

    override suspend fun updateSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
        walletItemId: UUID,
        amount: BigDecimal,
        periodicity: RecurrenceType,
        nextExecution: LocalDate,
        qtyLimit: Int?,
        removesAllocation: Boolean,
    ): GoalContributionScheduleLine {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val existing =
            scheduleRepository
                .findByIdAndFinancialGoalId(scheduleId, goalId)
                .awaitSingleOrNull() ?: throw FinancialGoalScheduleNotFoundException(scheduleId)
        val wallet = resolveBankWallet(goal, walletItemId)
        ensureWalletCurrencyIsTargeted(goalId = goal.id!!, walletCurrency = wallet.currency)
        val amt = amount.asMoney()
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw FinancialGoalInvalidWalletException(walletItemId)
        }
        val endExecution =
            if (existing.qtyExecuted == 0) {
                recurrenceService.calculateEndDate(
                    lastExecution = nextExecution,
                    periodicity = periodicity,
                    qtyExecuted = 0,
                    qtyLimit = qtyLimit,
                )
            } else {
                val anchor = existing.lastExecution ?: nextExecution
                recurrenceService.calculateEndDate(
                    lastExecution = anchor,
                    periodicity = periodicity,
                    qtyExecuted = existing.qtyExecuted,
                    qtyLimit = qtyLimit,
                )
            }
        val updated =
            FinancialGoalContributionScheduleEntity(
                financialGoalId = goalId,
                walletItemId = wallet.id!!,
                amount = amt,
                currency = wallet.currency.uppercase(),
                periodicity = periodicity,
                qtyExecuted = existing.qtyExecuted,
                qtyLimit = qtyLimit,
                lastExecution = existing.lastExecution,
                nextExecution = nextExecution,
                endExecution = endExecution,
                removesAllocation = removesAllocation,
            ).also {
                it.id = existing.id
                it.createdAt = existing.createdAt
                it.updatedAt = OffsetDateTime.now(clock)
            }
        val saved = scheduleRepository.save(updated).awaitSingle()
        return toScheduleLine(
            saved,
            mapOf(
                wallet.id!! to
                    WalletInfo(
                        currency = wallet.currency.uppercase(Locale.ROOT),
                        name = wallet.name,
                    ),
            ),
        )
    }

    @Transactional
    override suspend fun deleteSchedule(
        userId: UUID,
        goalId: UUID,
        scheduleId: UUID,
    ) {
        val goal = loadGoal(goalId)
        ensureEditAccess(userId, goal)
        val schedule =
            scheduleRepository
                .findByIdAndFinancialGoalId(scheduleId, goalId)
                .awaitSingleOrNull() ?: return
        scheduleRepository.deleteById(schedule.id!!.toString()).then().awaitSingle()
    }

    @Transactional
    override suspend fun materializeDueSchedules() {
        val today = LocalDate.now(clock)
        val due = scheduleRepository.findAllByNextExecutionLessThanEqual(today).collectList().awaitSingle()
        for (schedule in due) {
            runCatching {
                materializeScheduleRow(schedule, today)
            }
        }
    }

    private suspend fun materializeScheduleRow(
        schedule: FinancialGoalContributionScheduleEntity,
        executionDate: LocalDate,
    ) {
        val goalId = schedule.financialGoalId
        val baseAmount = schedule.amount.asMoney()
        val signedAmount =
            if (schedule.removesAllocation) {
                baseAmount.negate()
            } else {
                baseAmount
            }
        val movement =
            FinancialGoalLedgerMovementEntity(
                financialGoalId = goalId,
                walletItemId = schedule.walletItemId,
                signedAmount = signedAmount,
                note = null,
                movementKind = GoalLedgerMovementKind.SCHEDULED_MATERIALIZATION,
                scheduleId = schedule.id,
                movementDate = executionDate,
            )
        ledgerMovementRepository.save(movement).awaitSingle()

        val qtyExecuted = schedule.qtyExecuted + 1
        val next =
            recurrenceService.calculateNextExecution(
                lastExecution = executionDate,
                periodicity = schedule.periodicity,
                qtyExecuted = qtyExecuted,
                qtyLimit = schedule.qtyLimit,
            )
        val updated =
            FinancialGoalContributionScheduleEntity(
                financialGoalId = schedule.financialGoalId,
                walletItemId = schedule.walletItemId,
                amount = schedule.amount,
                currency = schedule.currency,
                periodicity = schedule.periodicity,
                qtyExecuted = qtyExecuted,
                qtyLimit = schedule.qtyLimit,
                lastExecution = executionDate,
                nextExecution = next,
                endExecution = schedule.endExecution,
                removesAllocation = schedule.removesAllocation,
            ).also {
                it.id = schedule.id
                it.createdAt = schedule.createdAt
                it.updatedAt = OffsetDateTime.now(clock)
            }
        scheduleRepository.save(updated).awaitSingle()
    }

    private suspend fun persistMovement(
        goalId: UUID,
        walletItemId: UUID,
        signedAmount: BigDecimal,
        note: String?,
        kind: GoalLedgerMovementKind,
        scheduleId: UUID?,
        movementDate: LocalDate,
    ) {
        ledgerMovementRepository
            .save(
                FinancialGoalLedgerMovementEntity(
                    financialGoalId = goalId,
                    walletItemId = walletItemId,
                    signedAmount = signedAmount,
                    note = note,
                    movementKind = kind,
                    scheduleId = scheduleId,
                    movementDate = movementDate,
                ),
            ).awaitSingle()
    }

    private suspend fun loadGoal(goalId: UUID): FinancialGoalEntity =
        financialGoalRepository.findById(goalId.toString()).awaitSingleOrNull() ?: throw FinancialGoalNotFoundException(goalId)

    private suspend fun ensureViewAccess(
        userId: UUID,
        goal: FinancialGoalEntity,
    ) {
        when {
            goal.userId != null -> {
                if (goal.userId != userId) {
                    throw FinancialGoalForbiddenException()
                }
            }
            goal.groupId != null -> {
                groupRepository
                    .findOneByUserIdAndId(userId, goal.groupId!!)
                    .awaitSingleOrNull() ?: throw FinancialGoalForbiddenException()
            }
            else -> throw FinancialGoalNotFoundException(goal.id!!)
        }
    }

    private suspend fun ensureEditAccess(
        userId: UUID,
        goal: FinancialGoalEntity,
    ) {
        when {
            goal.userId != null -> {
                if (goal.userId != userId) {
                    throw FinancialGoalForbiddenException()
                }
            }
            goal.groupId != null -> {
                if (!groupPermissionService.hasPermission(userId, goal.groupId!!, GroupPermissions.MANAGE_GOALS)) {
                    throw FinancialGoalForbiddenException()
                }
            }
            else -> throw FinancialGoalNotFoundException(goal.id!!)
        }
    }

    private suspend fun resolveBankWallet(
        goal: FinancialGoalEntity,
        walletItemId: UUID,
    ) = walletItemRepository.findOneById(walletItemId).awaitSingleOrNull().let { wallet ->
        if (wallet == null || wallet.type != WalletItemType.BANK_ACCOUNT) {
            throw FinancialGoalInvalidWalletException(walletItemId)
        }
        when {
            goal.userId != null -> {
                if (wallet.userId != goal.userId) {
                    throw FinancialGoalInvalidWalletException(walletItemId)
                }
            }
            goal.groupId != null -> {
                val linked =
                    groupWalletItemRepository
                        .countByGroupIdAndWalletItemId(goal.groupId!!, walletItemId)
                        .awaitSingle()
                if (linked == 0L) {
                    throw FinancialGoalInvalidWalletException(walletItemId)
                }
            }
        }
        wallet
    }

    private suspend fun ensureWalletCurrencyIsTargeted(
        goalId: UUID,
        walletCurrency: String,
    ) {
        val targetCurrencies =
            financialGoalTargetRepository
                .findAllByFinancialGoalId(goalId)
                .map { it.currency.uppercase(Locale.ROOT) }
                .collectList()
                .awaitSingle()
                .toSet()
        val normalizedWalletCurrency = walletCurrency.uppercase(Locale.ROOT)
        if (!targetCurrencies.contains(normalizedWalletCurrency)) {
            throw FinancialGoalUnsupportedCurrencyException(goalId = goalId, currency = normalizedWalletCurrency)
        }
    }

    private fun toHeader(entity: FinancialGoalEntity): FinancialGoalHeader =
        FinancialGoalHeader(
            id = entity.id!!,
            name = entity.name,
            description = entity.description,
            deadline = entity.deadline,
            ownerUserId = entity.userId,
            groupId = entity.groupId,
        )

    private fun toMovementLine(
        entity: FinancialGoalLedgerMovementEntity,
        currency: String,
        walletItemName: String,
    ): GoalLedgerMovementLine =
        GoalLedgerMovementLine(
            id = entity.id!!,
            walletItemId = entity.walletItemId,
            walletItemName = walletItemName,
            currency = currency,
            signedAmount = entity.signedAmount.asMoney(),
            note = entity.note,
            movementKind = entity.movementKind,
            scheduleId = entity.scheduleId,
            movementDate = entity.movementDate,
            createdAt = entity.createdAt,
        )

    private fun toScheduleLine(
        entity: FinancialGoalContributionScheduleEntity,
        walletInfoById: Map<UUID, WalletInfo>,
    ): GoalContributionScheduleLine {
        val name = walletInfoById[entity.walletItemId]?.name ?: entity.walletItemId.toString()
        return GoalContributionScheduleLine(
            id = entity.id!!,
            walletItemId = entity.walletItemId,
            walletItemName = name,
            amount = entity.amount.asMoney(),
            currency = entity.currency.uppercase(),
            periodicity = entity.periodicity,
            qtyExecuted = entity.qtyExecuted,
            qtyLimit = entity.qtyLimit,
            lastExecution = entity.lastExecution,
            nextExecution = entity.nextExecution,
            endExecution = entity.endExecution,
            removesAllocation = entity.removesAllocation,
        )
    }

    private fun buildCommitmentChart(
        goalEntity: FinancialGoalEntity,
        targets: List<FinancialGoalTargetAmount>,
        movementsAsc: List<FinancialGoalLedgerMovementEntity>,
        walletInfoById: Map<UUID, WalletInfo>,
    ): List<GoalCommitmentChartSeries> {
        if (targets.isEmpty()) {
            return emptyList()
        }

        val rows =
            movementsAsc.map { e ->
                val currency = walletInfoById[e.walletItemId]?.currency ?: "BRL"
                Triple(YearMonth.from(e.movementDate), e.signedAmount.asMoney(), currency)
            }

        val startCandidates = mutableListOf<YearMonth>()
        goalEntity.createdAt?.let { startCandidates.add(YearMonth.from(it.toLocalDate())) }
        rows.minOfOrNull { it.first }?.let { startCandidates.add(it) }
        if (startCandidates.isEmpty()) {
            startCandidates.add(YearMonth.from(LocalDate.now(clock)))
        }
        val rawStartYm = startCandidates.minOrNull()!!

        val endCandidates = mutableListOf(YearMonth.from(LocalDate.now(clock)))
        goalEntity.deadline?.let { endCandidates.add(YearMonth.from(it)) }
        rows.maxOfOrNull { it.first }?.let { endCandidates.add(it) }
        val rawEndYm = endCandidates.maxOrNull()!!

        val currentYm = YearMonth.from(LocalDate.now(clock))

        if (rawStartYm.isAfter(rawEndYm)) {
            val anchorStart = currentYm.minusMonths(2)
            return targets.map { t ->
                val c = t.currency.uppercase()
                val points =
                    (0..<COMMITMENT_CHART_MIN_MONTHS).map { offset ->
                        val ym = anchorStart.plusMonths(offset.toLong())
                        GoalCommitmentMonthlyPoint(
                            yearMonth = ym.toString(),
                            committedCumulative = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        )
                    }
                GoalCommitmentChartSeries(currency = c, targetAmount = t.targetAmount, points = points)
            }
        }

        // Include at least two calendar months before the current month, and at least two after (current in the middle → 5 months).
        val chartStartYm = minOf(rawStartYm, currentYm.minusMonths(2))
        var chartEndYm = maxOf(rawEndYm, currentYm.plusMonths(2))
        // At least COMMITMENT_CHART_MIN_MONTHS on the X axis so the line chart does not look cramped.
        chartEndYm = maxOf(chartEndYm, chartStartYm.plusMonths((COMMITMENT_CHART_MIN_MONTHS - 1).toLong()))

        val series = mutableListOf<GoalCommitmentChartSeries>()
        for (t in targets) {
            val c = t.currency.uppercase()
            val byYm = rows.filter { it.third == c }.groupBy { it.first }
            val points = mutableListOf<GoalCommitmentMonthlyPoint>()
            var running = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            var ym = chartStartYm
            while (!ym.isAfter(chartEndYm)) {
                val delta =
                    byYm[ym]?.fold(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) { acc, triple ->
                        acc.add(triple.second)
                    } ?: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                running = running.add(delta).setScale(2, RoundingMode.HALF_UP)
                points.add(GoalCommitmentMonthlyPoint(yearMonth = ym.toString(), committedCumulative = running))
                ym = ym.plusMonths(1)
            }
            series.add(GoalCommitmentChartSeries(currency = c, targetAmount = t.targetAmount, points = points))
        }
        return series
    }

    private data class WalletInfo(
        val currency: String,
        val name: String,
    )

    private fun BigDecimal.asMoney(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)
}
