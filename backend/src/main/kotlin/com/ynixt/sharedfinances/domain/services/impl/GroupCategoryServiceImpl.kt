package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.GroupCategoryService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupCategoryServiceImpl(
    private val categoryRepository: WalletEntryCategoryRepository,
) : GroupCategoryService {
    override fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): Mono<List<WalletEntryCategory>> =
        categoryRepository
            .saveAll(
                categories.map {
                    WalletEntryCategory(
                        name = it.name,
                        parentId = it.parentId,
                        color = it.color,
                        groupId = groupId,
                        userId = null,
                    )
                },
            ).collectList()
}
