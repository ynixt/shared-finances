package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletCategoryConceptRepository : EntityRepository<WalletCategoryConceptEntity> {
    fun findOneByCode(code: WalletCategoryConceptCode): Mono<WalletCategoryConceptEntity>

    fun findAllAvailableForUser(userId: UUID): Flux<WalletCategoryConceptEntity>

    fun deleteCustomIfOrphaned(id: UUID): Mono<Long>
}
