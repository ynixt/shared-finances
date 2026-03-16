package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupInviteEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupInviteService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GroupInviteServiceImpl(
    private val repository: GroupInviteRepository,
    private val groupPermissionService: GroupPermissionService,
    private val groupService: GroupService,
    @Value("\${app.invitationExpirationMinutes}") private val invitationExpirationMinutes: Long,
) : GroupInviteService {
    override suspend fun generate(
        userId: UUID,
        groupId: UUID,
    ): GroupInviteEntity? =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                permission = GroupPermissions.ADD_MEMBER,
            ).let { hasPermission ->
                if (hasPermission) {
                    repository
                        .save(
                            GroupInviteEntity(
                                groupId = groupId,
                                expireAt = OffsetDateTime.now().plusMinutes(invitationExpirationMinutes),
                            ),
                        ).awaitSingle()
                } else {
                    null
                }
            }

    override suspend fun expireOld(): Long = repository.deleteAllByExpireAtLessThanEqual(OffsetDateTime.now()).awaitSingle()

    override suspend fun findInfoForInvite(inviteId: UUID): GroupInfoForInvite? = repository.findInfoForInvite(inviteId).awaitSingleOrNull()

    @Transactional
    override suspend fun accept(
        userId: UUID,
        inviteId: UUID,
    ): UUID? =
        repository.findById(inviteId).awaitSingleOrNull()?.let { invite ->
            repository.deleteOneByIdAndExpireAtGreaterThan(inviteId, OffsetDateTime.now()).awaitSingle().let {
                if (it > 0) {
                    groupService
                        .addNewMember(
                            userId = userId,
                            id = invite.groupId,
                        )

                    invite.groupId
                } else {
                    null
                }
            }
        }
}
