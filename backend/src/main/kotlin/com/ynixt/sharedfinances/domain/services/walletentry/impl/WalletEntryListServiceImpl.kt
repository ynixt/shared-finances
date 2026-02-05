package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryListService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEntryListServiceImpl(
    private val walletEntryRepository: WalletEntryRepository,
    private val walletItemMapper: WalletItemMapper,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val groupPermissionService: GroupPermissionService,
    private val userService: UserService,
    private val entryRecurrenceConfigService: EntryRecurrenceConfigService,
) : WalletEntryListService {
    override suspend fun list(
        userId: UUID,
        groupId: UUID?,
        request: ListEntryRequest,
    ): CursorPage<EntryListResponse> {
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
        var simulatedEntries: List<EntryListResponse> = emptyList()

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
            walletEntryRepository
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
                            WalletEntryCursorFindAll(
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
        item: MinimumWalletEntry,
        simulateBillForRecurrence: Boolean,
    ): EntryListResponse =
        convertEntityToEntryListResponse(
            listOf(item),
            simulateBillForRecurrence,
        ).first()

    override suspend fun convertEntityToEntryListResponse(
        items: List<MinimumWalletEntry>,
        simulateBillForRecurrence: Boolean,
    ): List<EntryListResponse> {
        val categoriesIds = items.filter { it.categoryId != null }.map { it.categoryId!! }.toSet()
        val groupsIds = items.filter { it.groupId != null }.map { it.groupId!! }.toSet()

        val categories = genericCategoryService.findAllByIdIn(categoriesIds).toList()
        val groups = groupService.findAllByIdIn(groupsIds).toList()

        val walletEntries =
            (
                items.map { it.origin } +
                    items
                        .filter { it.target != null }
                        .map { it.target!! }
            ).map { walletItemMapper.toModel(it!!) }
                .toSet()

        val userIds = (items.map { it.userId } + walletEntries.map { it.userId }).filter { it != null }.mapNotNull { it }.toSet()

        return userService.findAllByIdIn(userIds).toList().let { users ->
            val walletEntriesById = walletEntries.associateBy { it.id!! }
            val categoriesById = categories.associateBy { it.id!! }
            val groupsById = groups.associateBy { it.id!! }
            val usersById = users.associateBy { it.id!! }

            items.map { entry ->
                createEntryListResponse(
                    entry = entry,
                    walletEntriesById = walletEntriesById,
                    categoriesById = categoriesById,
                    groupsById = groupsById,
                    usersById = usersById,
                    simulateBillForRecurrence = simulateBillForRecurrence,
                )
            }
        }
    }

    private suspend fun createEntryListResponse(
        entry: MinimumWalletEntry,
        walletEntriesById: Map<UUID, WalletItem>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
        groupsById: Map<UUID, GroupEntity>,
        usersById: Map<UUID, UserEntity>,
        simulateBillForRecurrence: Boolean,
    ): EntryListResponse =
        when (entry) {
            is WalletEntryEntity ->
                EntryListResponse(
                    id = entry.id!!,
                    type = entry.type,
                    name = entry.name,
                    value = entry.value,
                    category = entry.categoryId?.let { categoriesById[it] },
                    user = entry.userId?.let { usersById[it] },
                    group = entry.groupId?.let { groupsById[it] },
                    tags = entry.tags,
                    observations = entry.observations,
                    origin = entry.originId.let { walletEntriesById[it]!! }.also { it.user = usersById[it.userId] },
                    target = entry.targetId?.let { walletEntriesById[it] }?.also { it.user = usersById[it.userId] },
                    date = entry.date,
                    confirmed = entry.confirmed,
                    installment = entry.installment,
                    recurrenceConfigId = entry.recurrenceConfigId,
                    currency = entry.origin!!.currency,
                    originBillId = entry.originBillId,
                    originBillDate = entry.originBill?.billDate,
                    targetBillId = entry.targetBillId,
                    targetBillDate = entry.targetBill?.billDate,
                )

            is EntryRecurrenceConfigEntity ->
                entryRecurrenceConfigService.simulateGeneration(
                    config = entry,
                    origin = entry.originId.let { walletEntriesById[it]!! }.also { it.user = usersById[it.userId] },
                    target = entry.targetId?.let { walletEntriesById[it] }?.also { it.user = usersById[it.userId] },
                    user = entry.userId?.let { usersById[it] },
                    group = entry.groupId?.let { groupsById[it] },
                    category = entry.categoryId?.let { categoriesById[it] },
                    simulateBillForRecurrence = simulateBillForRecurrence,
                )

            else -> TODO()
        }
}
