package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEventListServiceImpl(
    private val walletEventRepository: WalletEventRepository,
    private val walletEntryRepository: WalletEntryRepository,
    private val recurrenceEntryRepository: RecurrenceEntryRepository,
    private val creditCardBillRepository: CreditCardBillRepository,
    private val walletItemMapper: WalletItemMapper,
    private val walletItemService: WalletItemService,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val groupPermissionService: GroupPermissionService,
    private val userService: UserService,
    private val recurrenceSeriesRepository: RecurrenceSeriesRepository,
    private val recurrenceSimulationService: RecurrenceSimulationService,
    private val recurrenceService: RecurrenceService,
    private val walletEventBeneficiaryRepository: WalletEventBeneficiarySpringDataRepository,
    private val recurrenceEventBeneficiaryRepository: RecurrenceEventBeneficiarySpringDataRepository,
) : WalletEventListService {
    override suspend fun list(
        userId: UUID,
        request: ListEntryRequest,
    ): CursorPage<EventListResponse> {
        if (!validateScopeBeforeSelect(userId, request)) {
            return CursorPage.empty()
        }

        val requestedWalletItemIds = (request.bankAccountIds + request.creditCardIds).toSet()
        val requestedEntryTypes = request.entryTypes
        val selectedCategoryConceptIds = resolveSelectedCategoryConceptIds(userId = userId, categoryIds = request.categoryIds)

        if (request.categoryIds.isNotEmpty() && selectedCategoryConceptIds.isEmpty() && !request.includeUncategorized) {
            return CursorPage.empty()
        }

        val queryScope = buildQueryScope(userId = userId, request = request)

        val now = LocalDate.now()
        var simulatedEntries: List<EventListResponse> = emptyList()

        if (!request.skipFuture || request.maximumDate != null && request.maximumDate > now) {
            simulatedEntries =
                if (request.billDate != null && request.walletItemId != null) {
                    recurrenceSimulationService.simulateGenerationForCreditCardWithFilters(
                        userId = userId,
                        walletItemId = request.walletItemId,
                        billDate = request.billDate,
                        groupIds = request.groupIds,
                        userIds = request.userIds,
                        walletItemIds = requestedWalletItemIds,
                        entryTypes = requestedEntryTypes,
                        categoryConceptIds = selectedCategoryConceptIds,
                        includeUncategorized = request.includeUncategorized,
                    )
                } else {
                    recurrenceSimulationService.simulateGenerationWithFilters(
                        userId = userId,
                        walletItemId = request.walletItemId,
                        minimumEndExecution = request.minimumDate,
                        maximumNextExecution = request.maximumDate,
                        billDate = null,
                        groupIds = request.groupIds,
                        userIds = request.userIds,
                        walletItemIds = requestedWalletItemIds,
                        entryTypes = requestedEntryTypes,
                        categoryConceptIds = selectedCategoryConceptIds,
                        includeUncategorized = request.includeUncategorized,
                    )
                }
        }

        val missingQtyAfterFuture = request.pageRequest.size - simulatedEntries.size

        if (missingQtyAfterFuture <= 0 || request.billDate != null && request.billId == null) {
            return CursorPage(
                items = simulatedEntries,
                hasNext = !(request.billDate != null && request.billId == null),
                nextCursor =
                    mapOf(
                        "skipFuture" to true,
                    ),
            )
        }

        val rawList =
            walletEventRepository
                .findAll(
                    scope = queryScope,
                    limit = missingQtyAfterFuture + 1,
                    walletItemId = request.walletItemId,
                    walletItemIds = requestedWalletItemIds,
                    entryTypes = requestedEntryTypes,
                    categoryConceptIds = selectedCategoryConceptIds,
                    includeUncategorized = request.includeUncategorized,
                    minimumDate = request.minimumDate,
                    maximumDate = request.maximumDate,
                    billId = request.billId,
                    cursor =
                        if (request.lastId != null && request.lastDate != null) {
                            WalletEventCursorFindAll(
                                maximumId = request.lastId,
                                maximumDate = request.lastDate,
                            )
                        } else {
                            null
                        },
                ).collectList()
                .awaitSingle()

        val hasNext = rawList.size > missingQtyAfterFuture

        return convertEntityToEntryListResponse(if (hasNext) rawList.subList(0, missingQtyAfterFuture) else rawList).let { items ->
            val lastItem = if (hasNext) items.lastOrNull() else null

            CursorPage(
                items = simulatedEntries + items,
                hasNext = hasNext,
                nextCursor =
                    lastItem?.let {
                        mapOf(
                            "id" to it.id!!,
                            "date" to it.date,
                        )
                    },
            )
        }
    }

    private suspend fun resolveSelectedCategoryConceptIds(
        userId: UUID,
        categoryIds: Set<UUID>,
    ): Set<UUID> {
        if (categoryIds.isEmpty()) {
            return emptySet()
        }

        val categories = genericCategoryService.findAllByIdIn(categoryIds).toList()
        if (categories.isEmpty()) {
            return emptySet()
        }

        val groupPermissionById =
            categories
                .mapNotNull { it.groupId }
                .toSet()
                .associateWith { groupId ->
                    groupPermissionService.hasPermission(
                        userId = userId,
                        groupId = groupId,
                    )
                }

        return categories
            .filter { category ->
                category.userId == userId ||
                    (category.groupId != null && groupPermissionById[category.groupId] == true)
            }.map { it.conceptId }
            .toSet()
    }

    private suspend fun validateScopeBeforeSelect(
        userId: UUID,
        request: ListEntryRequest,
    ): Boolean {
        if (request.groupIds.isEmpty()) {
            return request.userIds.isEmpty()
        }

        return request.groupIds.none { groupId ->
            !groupPermissionService.hasPermission(
                userId = userId,
                groupId = groupId,
            )
        }
    }

    private fun buildQueryScope(
        userId: UUID,
        request: ListEntryRequest,
    ): WalletTransactionQueryScope =
        when {
            request.groupIds.isEmpty() ->
                WalletTransactionQueryScope.ownership(
                    ownerUserIds = setOf(userId),
                )
            request.userIds.isNotEmpty() ->
                WalletTransactionQueryScope.ownership(
                    ownerUserIds = request.userIds,
                    groupIds = request.groupIds,
                )
            else -> WalletTransactionQueryScope.group(groupIds = request.groupIds)
        }

    override suspend fun findById(
        userId: UUID,
        walletEventId: UUID,
    ): EventListResponse? {
        val event = walletEventRepository.findById(walletEventId).awaitSingleOrNull() ?: return null

        val hasReadPermission =
            when {
                event.groupId != null ->
                    groupPermissionService.hasPermission(
                        userId = userId,
                        groupId = event.groupId,
                    )

                else -> event.createdByUserId == userId
            }

        if (!hasReadPermission) {
            return null
        }

        event.entries =
            walletEntryRepository
                .findAllByWalletEventId(walletEventId)
                .asFlow()
                .toList()
                .sortedBy { it.value }
                .also { loadedEntries ->
                    hydrateWalletItems(loadedEntries)
                    hydrateBills(loadedEntries)
                    loadedEntries.forEach { entry ->
                        entry.event = event
                    }
                }
        event.beneficiaries =
            walletEventBeneficiaryRepository
                .findAllByWalletEventId(walletEventId)
                .asFlow()
                .toList()
                .also { loaded ->
                    loaded.forEach { beneficiary ->
                        beneficiary.event = event
                    }
                }

        return convertEntityToEntryListResponse(event)
    }

    override suspend fun findScheduledByRecurrenceConfigId(
        userId: UUID,
        recurrenceConfigId: UUID,
    ): EventListResponse? {
        val config = recurrenceService.findAllByIdIn(setOf(recurrenceConfigId)).toList().firstOrNull() ?: return null
        hydrateSeriesTotals(listOf(config))

        val hasReadPermission =
            when {
                config.groupId != null ->
                    groupPermissionService.hasPermission(
                        userId = userId,
                        groupId = config.groupId,
                    )

                else -> config.createdByUserId == userId
            }

        if (!hasReadPermission || config.nextExecution == null) {
            return null
        }

        val recurrenceEntries =
            recurrenceEntryRepository
                .findAllByWalletEventId(recurrenceConfigId)
                .asFlow()
                .toList()
                .sortedBy { it.value }

        if (recurrenceEntries.isEmpty()) {
            return null
        }

        config.entries =
            recurrenceEntries.also { loadedEntries ->
                loadedEntries.forEach { entry ->
                    entry.event = config
                }
            }
        config.beneficiaries =
            recurrenceEventBeneficiaryRepository
                .findAllByWalletEventId(recurrenceConfigId)
                .asFlow()
                .toList()
                .also { loaded ->
                    loaded.forEach { beneficiary ->
                        beneficiary.event = config
                    }
                }

        val walletItemsById =
            walletItemService
                .findAllByIdIn(recurrenceEntries.map { entry -> entry.walletItemId }.toSet())
                .toList()
                .associateBy { item -> item.id!! }
        val walletItems = recurrenceEntries.mapNotNull { entry -> walletItemsById[entry.walletItemId] }

        if (walletItems.size != recurrenceEntries.size) {
            return null
        }

        val usersById =
            userService
                .findAllByIdIn(setOf(config.createdByUserId))
                .toList()
                .associateBy { user -> user.id!! }
        val groupsById =
            groupService
                .findAllByIdIn(setOfNotNull(config.groupId))
                .toList()
                .associateBy { group -> group.id!! }
        val categoriesById =
            genericCategoryService
                .findAllByIdIn(setOfNotNull(config.categoryId))
                .toList()
                .associateBy { category -> category.id!! }

        val nextBillDateByWalletItemId = recurrenceEntries.associate { entry -> entry.walletItemId to entry.nextBillDate }

        return recurrenceSimulationService
            .simulateGeneration(
                config = config,
                walletItems = walletItems,
                user = usersById[config.createdByUserId],
                group = config.groupId?.let { groupsById[it] },
                category = config.categoryId?.let { categoriesById[it] },
                simulateBillForRecurrence = false,
            ).let { simulated ->
                simulated.copy(
                    entries =
                        simulated.entries.map { entry ->
                            entry.copy(billDate = nextBillDateByWalletItemId[entry.walletItemId])
                        },
                )
            }
    }

    override suspend fun convertEntityToEntryListResponse(
        event: MinimumWalletEventEntity,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse =
        convertEntityToEntryListResponse(
            listOf(event),
            simulateBillForRecurrence,
        ).first()

    override suspend fun convertEntityToEntryListResponse(
        events: List<MinimumWalletEventEntity>,
        simulateBillForRecurrence: Boolean,
    ): List<EventListResponse> {
        val categoriesIds = events.filter { it.categoryId != null }.map { it.categoryId!! }.toSet()
        val groupsIds = events.filter { it.groupId != null }.map { it.groupId!! }.toSet()
        val recurrenceConfigIds =
            events
                .filter {
                    it is WalletEventEntity && it.recurrenceEventId != null
                }.map { (it as WalletEventEntity).recurrenceEventId!! }
                .toSet()

        val categories = genericCategoryService.findAllByIdIn(categoriesIds).toList()
        val groups = groupService.findAllByIdIn(groupsIds).toList()
        val recurrenceConfigs = recurrenceService.findAllByIdIn(recurrenceConfigIds).toList()
        hydrateSeriesTotals(recurrenceConfigs)
        val walletEntries =
            (
                events.flatMap { it.entries!! }.filter { it.walletItem != null }.map { it.walletItem }
            ).map { walletItemMapper.toModel(it!!) }
                .toSet()
        val userIds = (events.map { it.createdByUserId } + walletEntries.map { it.userId }).toSet()

        return userService.findAllByIdIn(userIds).toList().let { users ->
            val walletEntriesById = walletEntries.associateBy { it.id!! }
            val categoriesById = categories.associateBy { it.id!! }
            val groupsById = groups.associateBy { it.id!! }
            val usersById = users.associateBy { it.id!! }
            val recurrenceConfigById = recurrenceConfigs.associateBy { it.id!! }

            events.map { entry ->
                createEntryListResponse(
                    event = entry,
                    walletItemById = walletEntriesById,
                    categoriesById = categoriesById,
                    groupsById = groupsById,
                    usersById = usersById,
                    recurrenceConfigById = recurrenceConfigById,
                    simulateBillForRecurrence = simulateBillForRecurrence,
                )
            }
        }
    }

    private suspend fun hydrateSeriesTotals(configs: List<RecurrenceEventEntity>) {
        if (configs.isEmpty()) {
            return
        }

        val seriesById =
            recurrenceSeriesRepository
                .findAllByIdIn(configs.map { it.seriesId }.toSet())
                .collectList()
                .awaitSingle()
                .associateBy { it.id!! }

        configs.forEach { config ->
            config.seriesQtyTotal = seriesById[config.seriesId]?.qtyTotal
        }
    }

    private suspend fun createEntryListResponse(
        event: MinimumWalletEventEntity,
        walletItemById: Map<UUID, WalletItem>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
        groupsById: Map<UUID, GroupEntity>,
        usersById: Map<UUID, UserEntity>,
        recurrenceConfigById: Map<UUID, RecurrenceEventEntity>,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse {
        val entries = event.entries!!

        walletItemById.forEach { entry -> entry.value.user = usersById[entry.value.userId] }

        val originEntry = entries.first()
        val targetEntry = entries.getOrNull(1)
        val originValue = if (event.type == WalletEntryType.TRANSFER) originEntry.value.abs() else null
        val targetValue = if (event.type == WalletEntryType.TRANSFER) targetEntry?.value?.abs() else null

        val user = usersById[event.createdByUserId]
        val group = event.groupId?.let { groupsById[it] }
        val category = event.categoryId?.let { categoriesById[it] }

        return when (event) {
            is WalletEventEntity -> {
                EventListResponse(
                    id = event.id!!,
                    type = event.type,
                    name = event.name,
                    entries =
                        entries.map { originalEntry ->
                            val entry = (originalEntry as WalletEntryEntity)
                            val walletItem = walletItemById[entry.walletItemId]!!

                            EventListResponse.EntryResponse(
                                value = entry.value,
                                walletItemId = entry.walletItemId,
                                walletItem = walletItem,
                                billDate = entry.bill?.billDate,
                                billId = entry.billId,
                                contributionPercent = entry.contributionPercent,
                            )
                        },
                    category = category,
                    user = user,
                    group = group,
                    tags = event.tags,
                    observations = event.observations,
                    date = event.date,
                    confirmed = event.confirmed,
                    installment = event.installment,
                    recurrenceConfigId = event.recurrenceEventId,
                    recurrenceConfig = event.recurrenceEventId?.let { recurrenceConfigById[it] },
                    currency = requireNotNull(walletItemById[entries.first().walletItemId]).currency,
                    transferPurpose = event.transferPurpose,
                    originValue = originValue,
                    targetValue = targetValue,
                    beneficiaries =
                        event.beneficiaries
                            ?.map { beneficiary ->
                                EventListResponse.BeneficiaryResponse(
                                    userId = beneficiary.beneficiaryUserId,
                                    benefitPercent = beneficiary.benefitPercent,
                                )
                            }.orEmpty(),
                )
            }

            is RecurrenceEventEntity ->
                recurrenceSimulationService.simulateGeneration(
                    config = event,
                    walletItems = entries.map { walletItemById[it.walletItemId]!! },
                    user = user,
                    group = group,
                    category = category,
                    simulateBillForRecurrence = simulateBillForRecurrence,
                )

            else -> TODO()
        }
    }

    private suspend fun hydrateWalletItems(entries: List<MinimumWalletEntryEntity>) {
        val entriesNeedingWalletItemHydration = entries.filter { it.walletItem == null }

        if (entriesNeedingWalletItemHydration.isEmpty()) {
            return
        }

        val walletItemsById =
            walletItemService
                .findAllByIdIn(entriesNeedingWalletItemHydration.map { it.walletItemId }.toSet())
                .toList()
                .associateBy { it.id!! }

        entriesNeedingWalletItemHydration.forEach { entry ->
            entry.walletItem = walletItemsById[entry.walletItemId]?.let(walletItemMapper::fromModel)
        }
    }

    private suspend fun hydrateBills(entries: List<MinimumWalletEntryEntity>) {
        val walletEntriesNeedingBillHydration =
            entries
                .filterIsInstance<WalletEntryEntity>()
                .filter { it.bill == null && it.billId != null }

        if (walletEntriesNeedingBillHydration.isEmpty()) {
            return
        }

        val billIds = walletEntriesNeedingBillHydration.mapNotNull { it.billId }.toSet()
        val billsById =
            creditCardBillRepository
                .findAllByIdIn(billIds)
                .collectList()
                .awaitSingle()
                .associateBy { it.id!! }

        walletEntriesNeedingBillHydration.forEach { entry ->
            entry.bill = entry.billId?.let { billsById[it] }
        }
    }
}
