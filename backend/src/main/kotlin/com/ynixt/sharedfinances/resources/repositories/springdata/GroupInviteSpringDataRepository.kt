package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupInvite
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupInviteSpringDataRepository :
    GroupInviteRepository,
    Repository<GroupInvite, String> {
    @Query(
        """
            select
                gi.id,
                g.name as group_name,
                gi.expire_at
            from group_invite gi
            join "group" g on g.id = gi.group_id
            where
                gi.id = :inviteId
                and expire_at > CURRENT_TIMESTAMP
        """,
    )
    override fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite>
}
