package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import com.ynixt.sharedfinances.domain.services.GroupInviteService
import com.ynixt.sharedfinances.domain.services.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.GroupService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GroupInviteServiceImpl(
    private val repository: GroupInviteRepository,
    private val groupPermissionService: GroupPermissionService,
    private val groupService: GroupService,
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
                permission = GroupPermissions.ADD_MEMBER,
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

    override fun findInfoForInvite(inviteId: UUID): Mono<GroupInfoForInvite> = repository.findInfoForInvite(inviteId)

    @Transactional
    override fun accept(
        userId: UUID,
        inviteId: UUID,
    ): Mono<UUID> =
        repository.findById(inviteId).flatMap { invite ->
            repository.deleteOneByIdAndExpireAtGreaterThan(inviteId, OffsetDateTime.now()).flatMap {
                if (it > 0) {
                    groupService
                        .addNewMember(
                            userId = userId,
                            id = invite.groupId,
                        ).thenReturn(invite.groupId)
                } else {
                    Mono.empty()
                }
            }
        }
}
