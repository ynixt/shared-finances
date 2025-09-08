package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.GroupUser
import org.springframework.data.repository.CrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupUsersSpringDataRepository : CrudRepository<GroupUser, String> {
    fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun save(groupUser: GroupUser): Mono<GroupUser>
}
