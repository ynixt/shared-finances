package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryCategoryRepository : EntityRepository<WalletEntryCategoryEntity> {
    fun saveAll(category: Iterable<WalletEntryCategoryEntity>): Flux<WalletEntryCategoryEntity>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun deleteByIdAndGroupId(
        id: UUID,
        groupId: UUID,
    ): Mono<Long>

    fun countByUserId(userId: UUID): Mono<Long>

    fun countByGroupId(userId: UUID): Mono<Long>

    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByGroupId(
        groupId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByUserIdAndNameStartsWith(
        userId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByGroupIdAndNameStartsWith(
        groupId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByUserIdAndParentIdIsNull(
        userId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByGroupIdAndParentIdIsNull(
        groupId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByUserIdAndParentIdIsNullAndNameStartsWith(
        userId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByGroupIdAndParentIdIsNullAndNameStartsWith(
        groupId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategoryEntity>

    fun findAllByParentIdIn(parentId: Collection<UUID>): Flux<WalletEntryCategoryEntity>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<WalletEntryCategoryEntity>

    fun findOneByIdAndGroupId(
        id: UUID,
        groupId: UUID,
    ): Mono<WalletEntryCategoryEntity>

    fun updateByUserId(
        id: UUID,
        userId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
    ): Mono<Long>

    fun updateByGroupId(
        id: UUID,
        groupId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
    ): Mono<Long>
}
