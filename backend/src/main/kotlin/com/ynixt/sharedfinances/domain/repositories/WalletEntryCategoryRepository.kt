package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEntryCategoryRepository {
    fun save(category: WalletEntryCategory): Mono<WalletEntryCategory>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun countByUserId(userId: UUID): Mono<Long>

    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategory>

    fun findAllByUserIdAndNameStartsWith(
        userId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategory>

    fun findAllByUserIdAndParentIdIsNull(
        userId: UUID,
        pageable: Pageable,
    ): Flux<WalletEntryCategory>

    fun findAllByUserIdAndParentIdIsNullAndNameStartsWith(
        userId: UUID,
        pageable: Pageable,
        name: String,
    ): Flux<WalletEntryCategory>

    fun findAllByParentIdIn(parentId: Collection<UUID>): Flux<WalletEntryCategory>

    fun findAllByGroupId(groupId: UUID): Flux<WalletEntryCategory>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<WalletEntryCategory>

    fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newColor: String,
        newParentId: UUID?,
    ): Mono<WalletEntryCategory>
}
