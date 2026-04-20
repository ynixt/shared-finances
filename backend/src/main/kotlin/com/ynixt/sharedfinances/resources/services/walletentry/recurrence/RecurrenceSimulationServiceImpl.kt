package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.plus
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceOccurrenceSimulationService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class RecurrenceSimulationServiceImpl(
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
    private val walletItemService: WalletItemService,
    private val creditCardBillService: CreditCardBillService,
    private val walletItemMapper: WalletItemMapper,
    private val recurrenceService: RecurrenceService,
    private val recurrenceOccurrenceSimulationService: RecurrenceOccurrenceSimulationService,
    private val recurrenceSeriesRepository: RecurrenceSeriesRepository,
    private val clock: Clock,
) : RecurrenceSimulationService {
    private val defaultSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

    override suspend fun getFutureValuesOfWalletItem(
        walletItemId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal =
        recurrenceService
            .findAllEntryByWalletId(
                minimumEndExecution = fixStartDate(minimumEndExecution),
                maximumNextExecution = maximumNextExecution,
                walletItemId = walletItemId,
                userId = if (groupId == null) userId else null,
                groupId = groupId,
            ).toList()
            .sumOf { it.entries?.find { e -> e.walletItemId == walletItemId }?.value ?: BigDecimal.ZERO }

    override suspend fun getFutureValuesOCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): BigDecimal =
        simulateGenerationForCreditCard(
            bill = bill,
            walletItemId = walletItemId,
            userId = userId,
            groupIds = if (groupId == null) emptySet() else setOf(groupId),
            userIds = emptySet(),
        ).sumOf { it.entries.filter { entry -> entry.walletItemId == walletItemId }.sumOf { entry -> entry.value } }

    override suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemId: UUID,
    ): List<EventListResponse> =
        simulateGenerationForCreditCardWithFilters(
            billDate = billDate,
            userId = userId,
            groupIds = groupIds,
            userIds = userIds,
            walletItemId = walletItemId,
            walletItemIds = emptySet(),
            entryTypes = emptySet(),
            categoryConceptIds = emptySet(),
            includeUncategorized = false,
        )

    override suspend fun simulateGenerationForCreditCardWithFilters(
        billDate: LocalDate,
        userId: UUID,
        walletItemId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): List<EventListResponse> {
        val bill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItemId,
                billDate = billDate,
            )

        return simulateGenerationForCreditCard(
            bill = bill,
            userId = userId,
            walletItemId = walletItemId,
            groupIds = groupIds,
            userIds = userIds,
            walletItemIds = walletItemIds,
            entryTypes = entryTypes,
            categoryConceptIds = categoryConceptIds,
            includeUncategorized = includeUncategorized,
        )
    }

    override suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemId: UUID?,
    ): List<EventListResponse> =
        simulateGeneration(
            minimumEndExecution = null,
            maximumNextExecution = null,
            userId = userId,
            groupIds = groupIds,
            userIds = userIds,
            walletItemId = walletItemId,
            billDate = bill.billDate,
        )

    private suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        walletItemId: UUID?,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): List<EventListResponse> =
        simulateGenerationWithFilters(
            minimumEndExecution = null,
            maximumNextExecution = null,
            userId = userId,
            walletItemId = walletItemId,
            billDate = bill.billDate,
            groupIds = groupIds,
            userIds = userIds,
            walletItemIds = walletItemIds,
            entryTypes = entryTypes,
            categoryConceptIds = categoryConceptIds,
            includeUncategorized = includeUncategorized,
        )

    override suspend fun simulateGeneration(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemId: UUID?,
        billDate: LocalDate?,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): List<EventListResponse> =
        simulateGenerationWithFilters(
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            userId = userId,
            walletItemId = walletItemId,
            billDate = billDate,
            groupIds = groupIds,
            userIds = userIds,
            walletItemIds = emptySet(),
            entryTypes = emptySet(),
            categoryConceptIds = categoryConceptIds,
            includeUncategorized = includeUncategorized,
        )

    override suspend fun simulateGenerationWithFilters(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        walletItemId: UUID?,
        billDate: LocalDate?,
        groupIds: Set<UUID>,
        userIds: Set<UUID>,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        categoryConceptIds: Set<UUID>,
        includeUncategorized: Boolean,
    ): List<EventListResponse> {
        require(groupIds.isNotEmpty() || userIds.isEmpty()) {
            "Filter userIds requires at least one groupId."
        }

        val fixedStartDate = fixStartDate(minimumEndExecution)
        val ownerUserIds =
            when {
                userIds.isNotEmpty() -> userIds
                groupIds.isEmpty() -> setOfNotNull(userId)
                else -> emptySet()
            }

        val configs =
            recurrenceService
                .findAllEntries(
                    minimumEndExecution = fixedStartDate,
                    maximumNextExecution = maximumNextExecution,
                    billDate = billDate,
                    walletItemId = walletItemId,
                    walletItemIds = walletItemIds,
                    userIds = ownerUserIds,
                    groupIds = groupIds,
                    entryTypes = entryTypes,
                    categoryConceptIds = categoryConceptIds,
                    includeUncategorized = includeUncategorized,
                    sort = defaultSort,
                ).toList()

        return simulateGenerationForConfigs(
            configs = configs,
            minimumDate = fixedStartDate,
            maximumDate = maximumNextExecution,
            askedBillDate = billDate,
            askedWalletItemId = walletItemId,
            requestUserId = userId,
        )
    }

    override suspend fun simulateGenerationForUsers(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userIds: Set<UUID>,
        billDate: LocalDate?,
    ): List<EventListResponse> {
        val fixedStartDate = fixStartDate(minimumEndExecution)
        if (userIds.isEmpty()) {
            return emptyList()
        }

        val configs =
            recurrenceService
                .findAllEntryByUserIds(
                    minimumEndExecution = fixedStartDate,
                    maximumNextExecution = maximumNextExecution,
                    userIds = userIds,
                    sort = defaultSort,
                ).toList()

        return simulateGenerationForConfigs(
            configs = configs,
            minimumDate = fixedStartDate,
            maximumDate = maximumNextExecution,
            askedBillDate = billDate,
            askedWalletItemId = null,
            requestUserId = null,
        )
    }

    override suspend fun simulateGenerationAsEntrySumResult(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        summaryMinimumDate: LocalDate,
    ): List<EntrySumResult> {
        val resultByWalletId = mutableMapOf<UUID, EntrySumResult>()
        val today = LocalDate.now(clock)

        simulateGeneration(
            userId = userId,
            groupIds = if (groupId == null) emptySet() else setOf(groupId),
            userIds = emptySet(),
            walletItemId = walletItemId,
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            billDate = null,
        ).forEach {
            val executionDate = it.date
            it.entries.forEach { entry ->
                if (!resultByWalletId.containsKey(entry.walletItemId)) {
                    resultByWalletId[entry.walletItemId] = EntrySumResult.empty(entry.walletItemId)
                }

                val movement =
                    EntrySum(
                        balance = entry.value,
                        revenue = if (entry.value >= BigDecimal.ZERO) entry.value else BigDecimal.ZERO,
                        expense = if (entry.value < BigDecimal.ZERO) entry.value.negate() else BigDecimal.ZERO,
                    )

                val isFuture = executionDate.isAfter(today)
                val carryToBalance = isFuture && executionDate.isBefore(summaryMinimumDate)
                val (sumPart, projectedPart) =
                    if (carryToBalance) {
                        movement to EntrySum.EMPTY
                    } else {
                        EntrySum.EMPTY to movement
                    }

                resultByWalletId[entry.walletItemId] = resultByWalletId[entry.walletItemId]!! +
                    EntrySumResult(
                        sum = sumPart,
                        period = EntrySum.EMPTY,
                        projected = projectedPart,
                        walletItemId = entry.walletItemId,
                        creditCardBillId = null,
                    ).also { result ->
                        result.walletItem = entry.walletItem
                        result.user = it.user
                    }
            }
        }

        return resultByWalletId.values.toList()
    }

    private suspend fun simulateGeneration(
        configs: List<RecurrenceEventEntity>,
        walletItemsById: Map<UUID, WalletItem>,
        userById: Map<UUID, UserEntity>,
        groupById: Map<UUID, GroupEntity>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
        requestUserId: UUID?,
    ): List<EventListResponse> {
        val simulatedEntries = mutableListOf<EventListResponse>()

        configs.forEach {
            val entries = it.entries!!
            val originEntry = entries.first()
            val targetEntry = entries.getOrNull(1)

            val originWalletItem = walletItemsById[originEntry.walletItemId]!!
            val targetWalletItem = targetEntry?.walletItemId?.let { targetId -> walletItemsById[targetId]!! }
            val user = it.hydratedUser ?: userById[it.createdByUserId]
            val group = it.hydratedGroup ?: it.groupId?.let { groupId -> groupById[groupId]!! }
            val category = it.hydratedCategory ?: it.categoryId?.let { categoryId -> categoriesById[categoryId]!! }
            val walletItems = listOfNotNull(originWalletItem, targetWalletItem)
            val occurrences =
                recurrenceOccurrenceSimulationService.buildOccurrences(
                    config = it,
                    walletItems = walletItems,
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                    askedBillDate = askedBillDate,
                    askedWalletItemId = askedWalletItemId,
                    requestUserId = requestUserId,
                )

            occurrences.forEach { occurrence ->
                simulatedEntries.add(
                    simulateGeneration(
                        config = it,
                        walletItems = walletItems,
                        user = user,
                        group = group,
                        category = category,
                        simulateBillForRecurrence = false,
                        simulatedDate = occurrence.executionDate,
                        simulatedInstallment = occurrence.installment,
                        simulatedBillDateByWalletItemId = occurrence.billDateByWalletItemId,
                    ),
                )
            }
        }
        return simulatedEntries
    }

    override suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse =
        simulateGeneration(
            config = config,
            walletItems = walletItems,
            user = user,
            group = group,
            category = category,
            simulateBillForRecurrence = simulateBillForRecurrence,
            simulatedDate = null,
            simulatedInstallment = null,
            simulatedBillDateByWalletItemId = null,
        )

    private suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
        simulatedDate: LocalDate?,
        simulatedInstallment: Int?,
        simulatedBillDateByWalletItemId: Map<UUID, LocalDate?>?,
    ): EventListResponse {
        val installment =
            if (simulatedInstallment != null) {
                simulatedInstallment
            } else if (config.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                config.seriesOffset + config.qtyExecuted + 1
            } else {
                null
            }

        val executionDate = simulatedDate ?: config.nextExecution!!

        val bills =
            walletItems.map {
                if (simulatedBillDateByWalletItemId != null) {
                    null
                } else if (simulateBillForRecurrence &&
                    it.type == WalletItemType.CREDIT_CARD
                ) {
                    simulateBill(it, executionDate, config.createdByUserId)
                } else {
                    null
                }
            }

        return EventListResponse(
            id = null,
            type = config.type,
            name = config.name,
            transferPurpose = config.transferPurpose,
            originValue =
                if (config.type ==
                    com.ynixt.sharedfinances.domain.enums.WalletEntryType.TRANSFER
                ) {
                    config.entries!!
                        .first()
                        .value
                        .abs()
                } else {
                    null
                },
            // Future transfer occurrences should not freeze the target-side amount before materialization.
            targetValue = null,
            beneficiaries =
                config.beneficiaries
                    ?.map { beneficiary ->
                        EventListResponse.BeneficiaryResponse(
                            userId = beneficiary.beneficiaryUserId,
                            benefitPercent = beneficiary.benefitPercent,
                        )
                    }.orEmpty(),
            entries =
                config.entries!!.mapIndexed { index, entry ->
                    val bill = bills[index]

                    EventListResponse.EntryResponse(
                        value = entry.value,
                        walletItemId = entry.walletItemId,
                        walletItem = walletItems.find { wt -> wt.id == entry.walletItemId }!!,
                        billDate = simulatedBillDateByWalletItemId?.get(entry.walletItemId) ?: bill?.billDate,
                        billId = bill?.id,
                        contributionPercent = (entry as? RecurrenceEntryEntity)?.contributionPercent,
                    )
                },
            category = category,
            user = user,
            group = group,
            tags = emptyList(),
            date = executionDate,
            observations = config.observations,
            recurrenceConfigId = config.id!!,
            recurrenceConfig = config,
            confirmed = false,
            currency = walletItems.first().currency,
            installment = installment,
        )
    }

    private fun fixStartDate(startDate: LocalDate?): LocalDate? =
        LocalDate
            .now(clock)
            .plusDays(1)
            .let { minimum ->
                if (startDate == null || startDate.isBefore(minimum)) minimum else startDate
            }

    private suspend fun simulateBill(
        walletItem: WalletItem,
        billDate: LocalDate?,
        userId: UUID?,
    ): CreditCardBill? {
        if (billDate == null || userId == null) return null
        return if (walletItem is CreditCard) {
            val dueDate = walletItem.getDueDate(billDate)

            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItem.id!!,
                billDate = dueDate.withStartOfMonth(),
            )
        } else {
            null
        }
    }

    private suspend fun simulateGenerationForConfigs(
        configs: List<RecurrenceEventEntity>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
        requestUserId: UUID?,
    ): List<EventListResponse> {
        if (configs.isEmpty()) {
            return emptyList()
        }

        hydrateSeriesTotals(configs)

        val categoriesById = resolveCategoriesById(configs)
        val groupsById = resolveGroupsById(configs)
        val usersById = resolveUsersById(configs)
        val walletItemsById = resolveWalletItemsById(configs)

        return simulateGeneration(
            configs = configs,
            walletItemsById = walletItemsById,
            userById = usersById,
            categoriesById = categoriesById,
            groupById = groupsById,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            askedBillDate = askedBillDate,
            askedWalletItemId = askedWalletItemId,
            requestUserId = requestUserId,
        ).sortedWith(
            compareByDescending<EventListResponse> { it.date }
                .thenByDescending { it.id },
        )
    }

    private suspend fun hydrateSeriesTotals(configs: List<RecurrenceEventEntity>) {
        if (configs.isEmpty()) {
            return
        }

        val missingSeriesIds =
            configs
                .filter { it.seriesQtyTotal == null }
                .map { it.seriesId }
                .toSet()

        if (missingSeriesIds.isEmpty()) {
            return
        }

        val seriesById =
            recurrenceSeriesRepository
                .findAllByIdIn(missingSeriesIds)
                .collectList()
                .awaitSingle()
                .associateBy { it.id!! }

        configs.forEach { config ->
            if (config.seriesQtyTotal == null) {
                config.seriesQtyTotal = seriesById[config.seriesId]?.qtyTotal
            }
        }
    }

    private suspend fun resolveCategoriesById(configs: List<RecurrenceEventEntity>): Map<UUID, WalletEntryCategoryEntity> {
        val categoriesById =
            configs
                .mapNotNull { config -> config.hydratedCategory?.id?.let { it to config.hydratedCategory!! } }
                .toMap()

        val missingCategoryIds =
            configs
                .filter { it.hydratedCategory == null }
                .mapNotNull { it.categoryId }
                .toSet()

        if (missingCategoryIds.isEmpty()) {
            return categoriesById
        }

        val loaded =
            genericCategoryService
                .findAllByIdIn(missingCategoryIds)
                .toList()
                .associateBy { it.id!! }

        return categoriesById + loaded
    }

    private suspend fun resolveGroupsById(configs: List<RecurrenceEventEntity>): Map<UUID, GroupEntity> {
        val groupsById =
            configs
                .mapNotNull { config -> config.hydratedGroup?.id?.let { it to config.hydratedGroup!! } }
                .toMap()

        val missingGroupIds =
            configs
                .filter { it.hydratedGroup == null }
                .mapNotNull { it.groupId }
                .toSet()

        if (missingGroupIds.isEmpty()) {
            return groupsById
        }

        val loaded =
            groupService
                .findAllByIdIn(missingGroupIds)
                .toList()
                .associateBy { it.id!! }

        return groupsById + loaded
    }

    private suspend fun resolveUsersById(configs: List<RecurrenceEventEntity>): Map<UUID, UserEntity> {
        val usersById =
            configs
                .mapNotNull { config -> config.hydratedUser?.id?.let { it to config.hydratedUser!! } }
                .toMap()

        val missingUserIds =
            configs
                .filter { it.hydratedUser == null }
                .map { it.createdByUserId }
                .toSet()

        if (missingUserIds.isEmpty()) {
            return usersById
        }

        val loaded =
            userService
                .findAllByIdIn(missingUserIds)
                .toList()
                .associateBy { it.id!! }

        return usersById + loaded
    }

    private suspend fun resolveWalletItemsById(configs: List<RecurrenceEventEntity>): Map<UUID, WalletItem> {
        val walletItemsById = linkedMapOf<UUID, WalletItem>()

        configs
            .flatMap { it.entries.orEmpty() }
            .forEach { entry ->
                entry.walletItem?.let { walletItemEntity ->
                    walletItemsById.putIfAbsent(entry.walletItemId, walletItemMapper.toModel(walletItemEntity))
                }
            }

        val missingWalletItemIds =
            configs
                .flatMap { it.entries.orEmpty() }
                .map { it.walletItemId }
                .filterNot(walletItemsById::containsKey)
                .toSet()

        if (missingWalletItemIds.isEmpty()) {
            return walletItemsById
        }

        walletItemService
            .findAllByIdIn(missingWalletItemIds)
            .toList()
            .forEach { walletItem ->
                walletItemsById[walletItem.id!!] = walletItem
            }

        return walletItemsById
    }
}
