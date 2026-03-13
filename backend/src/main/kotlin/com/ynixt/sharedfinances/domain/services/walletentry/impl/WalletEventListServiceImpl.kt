package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEventListServiceImpl(
    private val walletEventRepository: WalletEventRepository,
    private val walletItemMapper: WalletItemMapper,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val groupPermissionService: GroupPermissionService,
    private val userService: UserService,
    private val entryRecurrenceConfigService: EntryRecurrenceConfigService,
) : WalletEventListService {
    override suspend fun list(
        userId: UUID,
        groupId: UUID?,
        request: ListEntryRequest,
    ): CursorPage<EventListResponse> {
        if (groupId != null) {
            if (!groupPermissionService.hasPermission(
                    userId = userId,
                    groupId = groupId,
                )
            ) {
                return CursorPage.empty()
            }
        }

        val now = LocalDate.now()
        var simulatedEntries: List<EventListResponse> = emptyList()

        if (!request.skipFuture || request.maximumDate != null && request.maximumDate > now) {
            simulatedEntries =
                if (request.billDate != null && request.walletItemId != null) {
                    entryRecurrenceConfigService.simulateGenerationForCreditCard(
                        userId = userId,
                        groupId = groupId,
                        walletItemId = request.walletItemId,
                        billDate = request.billDate,
                    )
                } else {
                    entryRecurrenceConfigService.simulateGeneration(
                        userId = userId,
                        groupId = groupId,
                        walletItemId = request.walletItemId,
                        minimumEndExecution = request.minimumDate,
                        maximumNextExecution = request.maximumDate,
                        billDate = null,
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
                    userId = if (groupId == null) userId else null,
                    groupId = groupId,
                    limit = missingQtyAfterFuture + 1,
                    walletItemId = request.walletItemId,
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
        val recurrenceConfigs = entryRecurrenceConfigService.findAllByIdIn(recurrenceConfigIds).toList()

        val walletEntries =
            (
                events.flatMap { it.entries!! }.filter { it.walletItem != null }.map { it.walletItem }
            ).map { walletItemMapper.toModel(it!!) }
                .toSet()

        val userIds = (events.map { it.userId } + walletEntries.map { it.userId }).filterNotNull().map { it }.toSet()

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

        val user = event.userId?.let { usersById[it] }
        val group = event.groupId?.let { groupsById[it] }
        val category = event.categoryId?.let { categoriesById[it] }

        return when (event) {
            is WalletEventEntity -> {
                val originEntryCasted = originEntry as WalletEntryEntity
                val targetEntryCasted = targetEntry as WalletEntryEntity?

                EventListResponse(
                    id = event.id!!,
                    type = event.type,
                    name = event.name,
                    entries =
                        entries.map { originalEntry ->
                            val entry = (originalEntry as WalletEntryEntity)

                            EventListResponse.EntryResponse(
                                value = entry.value,
                                walletItemId = entry.walletItemId,
                                walletItem = walletItemById[entry.walletItemId]!!,
                                billDate = entry.bill?.billDate,
                                billId = entry.billId,
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
                    currency = entries.first().walletItem!!.currency,
                )
            }

            is RecurrenceEventEntity ->
                entryRecurrenceConfigService.simulateGeneration(
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
}
