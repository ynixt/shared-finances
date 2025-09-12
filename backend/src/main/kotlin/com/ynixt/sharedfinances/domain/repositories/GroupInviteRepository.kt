package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

interface GroupInviteRepository {
    fun findById(id: UUID): Mono<GroupInvite>

    fun save(invite: GroupInvite): Mono<GroupInvite>

    fun deleteAllByExpireAtLessThanEqual(expireAt: OffsetDateTime): Mono<Long>

    fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite>

    fun deleteOneByIdAndExpireAtGreaterThan(
        id: UUID,
        expireAt: OffsetDateTime,
    ): Mono<Long>
}
