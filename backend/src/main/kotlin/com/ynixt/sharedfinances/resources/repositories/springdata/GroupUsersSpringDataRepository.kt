package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.CrudRepository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupUsersSpringDataRepository : CrudRepository<GroupUser, String> {
    fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long>

    fun findOneByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<GroupUser>

    @Modifying
    fun save(groupUser: GroupUser): Mono<GroupUser>

    @Modifying
    @Query(
        """
        update group_user
        set
            role = :newRole
        where
            group_id = :groupId
            and user_id = :userId
    """,
    )
    fun updateRole(
        userId: UUID,
        groupId: UUID,
        newRole: UserGroupRole,
    ): Mono<Long>
}
