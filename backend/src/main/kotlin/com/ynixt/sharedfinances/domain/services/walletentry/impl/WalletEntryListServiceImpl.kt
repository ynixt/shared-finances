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
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3
import java.util.UUID

@Service
class WalletEntryListServiceImpl(
    private val walletEntryRepository: WalletEntryRepository,
    private val walletItemMapper: WalletItemMapper,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
) : WalletEntryListService {
    override fun list(
        userId: UUID?,
        groupId: UUID?,
        request: ListEntryRequest,
    ): Mono<CursorPage<EntryListResponse>> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        val flux =
            walletEntryRepository.findAll(
                userId = userId,
                groupId = groupId,
                limit = request.pageRequest.size + 1,
                walletItemIs = request.walletItemIds,
                minimumDate = request.minimumDate,
                maximumDate = request.maximumDate,
                cursor =
                    if (request.lastId != null && request.lastDate != null) {
                        WalletEntryCursorFindAll(
                            maximumId = request.lastId,
                            maximumDate = request.lastDate,
                        )
                    } else {
                        null
                    },
            )

        return flux.collectList().flatMap { rawList ->
            val hasNext = rawList.size > request.pageRequest.size

            convertEntityToEntryListResponse(if (hasNext) rawList.subList(0, request.pageRequest.size) else rawList).map { items ->
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
    }

    private fun convertEntityToEntryListResponse(items: List<WalletEntryEntity>): Mono<List<EntryListResponse>> {
        val categoriesIds = items.filter { it.categoryId != null }.map { it.categoryId!! }.toSet()
        val groupsIds = items.filter { it.groupId != null }.map { it.groupId!! }.toSet()

        return Mono
            .zip(
                genericCategoryService.findAllByIdIn(categoriesIds).collectList().defaultIfEmpty(emptyList()),
                groupService.findAllByIdIn(groupsIds).collectList().defaultIfEmpty(emptyList()),
            ).flatMap { (categories, groups) ->
                val walletEntries =
                    (
                        items.map { it.origin } +
                            items
                                .filter { it.target != null }
                                .map { it.target!! }
                    ).map { walletItemMapper.toModel(it!!) }
                        .toSet()
                val userIds = (items.map { it.userId } + walletEntries.map { it.userId }).filter { it != null }.mapNotNull { it }.toSet()

                userService.findAllByIdIn(userIds).collectList().defaultIfEmpty(emptyList()).map { users ->
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
        )
}
