package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupSpringDataRepository :
    GroupRepository,
    R2dbcRepository<GroupEntity, String> {
    override fun findAllByOwnerUserId(ownerUserId: UUID): Flux<GroupEntity>

    @Query(
        """
            select g.*, gu.role as role, (g.owner_user_id = :userId) as is_owner
            from "group" g
            join group_user gu on gu.group_id = g.id
            where gu.user_id = :userId
            order by g.name
        """,
    )
    override fun findAllByUserIdOrderByName(userId: UUID): Flux<GroupWithRole>

    @Query(
        """
            select g.*, gu.role as role, (g.owner_user_id = :userId) as is_owner
            from "group" g
            join group_user gu on gu.group_id = g.id
            where
                g.id = :id
                and gu.user_id = :userId
        """,
    )
    override fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole>

    @Query(
        """
            select g.*, gu.role as role, (g.owner_user_id = :userId) as is_owner
            from "group" g
            join group_user gu on gu.group_id = g.id
            where
                gu.user_id = :userId
                and lower(g.name) like concat('%', lower(:name), '%')
            order by g.name
        """,
    )
    override fun searchByUserIdAndNameContainingIgnoreCase(
        userId: UUID,
        name: String,
    ): Flux<GroupWithRole>

    @Query(
        """
            select count(*)
            from "group" g
            join group_user gu on gu.group_id = g.id
            where
                gu.user_id = :userId
                and lower(g.name) like concat('%', lower(:name), '%')
        """,
    )
    override fun countByUserIdAndNameContainingIgnoreCase(
        userId: UUID,
        name: String,
    ): Mono<Long>

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

    @Modifying
    @Query(
        """
        update "group"
        set
            owner_user_id = :ownerUserId,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun updateOwnerUserId(
        id: UUID,
        ownerUserId: UUID,
    ): Mono<Long>
}
