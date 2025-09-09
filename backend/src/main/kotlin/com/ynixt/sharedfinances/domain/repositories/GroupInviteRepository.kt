package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

interface GroupInviteRepository {
    fun save(invite: GroupInvite): Mono<GroupInvite>

    fun deleteAllByExpireAtLessThanEqual(expireAt: OffsetDateTime): Mono<Long>
}
