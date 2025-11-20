package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.groups.GroupInviteEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface GroupInviteRepository {
    fun findById(id: UUID): Mono<GroupInviteEntity>

    fun save(invite: GroupInviteEntity): Mono<GroupInviteEntity>

    fun deleteAllByExpireAtLessThanEqual(expireAt: OffsetDateTime): Mono<Long>

    fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite>

    fun deleteOneByIdAndExpireAtGreaterThan(
        id: UUID,
        expireAt: OffsetDateTime,
    ): Mono<Long>
}
