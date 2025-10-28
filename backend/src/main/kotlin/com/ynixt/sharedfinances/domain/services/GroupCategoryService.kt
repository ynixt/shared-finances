package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupCategoryService {
    fun newCategories(
        groupId: UUID,
        categories: List<NewCategoryRequest>,
    ): Mono<List<WalletEntryCategory>>
}
