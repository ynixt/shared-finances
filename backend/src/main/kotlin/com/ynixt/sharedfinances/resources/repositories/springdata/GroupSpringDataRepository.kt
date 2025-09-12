package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupSpringDataRepository :
    GroupRepository,
    ReactiveCrudRepository<Group, String> {
    @Query(
        """
            select g.*, gu.role as role
            from "group" g
            join group_user gu on gu.group_id = g.id
            where gu.user_id = :userId
            order by g.name
        """,
    )
    override fun findAllByUserIdOrderByName(userId: UUID): Flux<GroupWithRole>

    @Query(
        """
            select g.*, gu.role as role
            from "group" g
            join group_user gu on gu.group_id = g.id
            where
                g.id = :groupId
                and gu.user_id = :userId
        """,
    )
    override fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    @Modifying
    @Query(
        """
        update "group"
        set
            name = :newName,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun edit(
        id: UUID,
        newName: String,
    ): Mono<Long>
}
