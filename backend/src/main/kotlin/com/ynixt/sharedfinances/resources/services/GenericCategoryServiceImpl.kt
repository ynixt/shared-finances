package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GenericCategoryServiceImpl(
    override val repository: WalletEntryCategoryRepository,
) : EntityServiceImpl<WalletEntryCategoryEntity, WalletEntryCategoryEntity>(),
    GenericCategoryService {
    override suspend fun findById(id: UUID): WalletEntryCategoryEntity? = repository.findById(id).awaitSingleOrNull()

    override suspend fun findAllByGroupIdAndConceptId(
        groupId: UUID,
        conceptId: UUID,
    ): List<WalletEntryCategoryEntity> =
        repository
            .findAllByGroupIdAndConceptId(
                groupId = groupId,
                conceptId = conceptId,
            ).collectList()
            .awaitSingle()
}
