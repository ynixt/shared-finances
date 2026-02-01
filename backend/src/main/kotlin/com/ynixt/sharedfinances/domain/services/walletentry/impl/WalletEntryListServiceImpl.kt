package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
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
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryListService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WalletEntryListServiceImpl(
    private val walletEntryRepository: WalletEntryRepository,
    private val walletItemMapper: WalletItemMapper,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
) : WalletEntryListService {
    override suspend fun list(
        userId: UUID?,
        groupId: UUID?,
        request: ListEntryRequest,
    ): CursorPage<EntryListResponse> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        val rawList =
            walletEntryRepository
                .findAll(
                    userId = userId,
                    groupId = groupId,
                    limit = request.pageRequest.size + 1,
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

        val hasNext = rawList.size > request.pageRequest.size

        // TODO: load future from EntryRecurrenceConfigService

        return convertEntityToEntryListResponse(if (hasNext) rawList.subList(0, request.pageRequest.size) else rawList).let { items ->
            val lastItem = if (hasNext) items.lastOrNull() else null

            CursorPage(
                items = items,
                hasNext = hasNext,
                nextCursor =
                    lastItem?.let {
                        mapOf(
                            "id" to it.id,
                            "date" to it.date,
                        )
                    },
            )
        }
    }

    private suspend fun convertEntityToEntryListResponse(items: List<WalletEntryEntity>): List<EntryListResponse> {
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
                )
            }
        }
    }

    private fun createEntryListResponse(
        entry: WalletEntryEntity,
        walletEntriesById: Map<UUID, WalletItem>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
        groupsById: Map<UUID, GroupEntity>,
        usersById: Map<UUID, UserEntity>,
    ): EntryListResponse =
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
        )
}
