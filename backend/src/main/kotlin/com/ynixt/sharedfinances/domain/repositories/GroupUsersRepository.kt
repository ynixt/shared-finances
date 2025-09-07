package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.GroupUsers
import reactor.core.publisher.Mono

interface GroupUsersRepository {
    fun save(groupUsers: GroupUsers): Mono<GroupUsers>
}
