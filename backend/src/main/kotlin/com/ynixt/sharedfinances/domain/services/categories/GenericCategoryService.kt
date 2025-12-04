package com.ynixt.sharedfinances.domain.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import reactor.core.publisher.Flux
import java.util.UUID

interface GenericCategoryService {
    fun findAllByIdIn(ids: Collection<UUID>): Flux<WalletEntryCategoryEntity>
}
