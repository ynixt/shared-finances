package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import com.ynixt.sharedfinances.domain.services.GroupInviteService
import com.ynixt.sharedfinances.domain.services.GroupPermissionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GroupInviteServiceImpl(
    private val repository: GroupInviteRepository,
    private val groupPermissionService: GroupPermissionService,
    @Value("\${app.invitationExpirationMinutes}") private val invitationExpirationMinutes: Long,
) : GroupInviteService {
    override fun generate(
        userId: UUID,
        groupId: UUID,
    ): Mono<GroupInvite> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                roleNeeded = UserGroupRole.ADMIN,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    repository.save(
                        GroupInvite(
                            groupId = groupId,
                            expireAt = OffsetDateTime.now().plusMinutes(invitationExpirationMinutes),
                        ),
                    )
                } else {
                    Mono.empty()
                }
            }

    override fun expireOld(): Mono<Long> = repository.deleteAllByExpireAtLessThanEqual(OffsetDateTime.now())
}
