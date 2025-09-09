package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.GroupService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupServiceImpl(
    private val groupRepository: GroupRepository,
    private val groupUserRepository: GroupUsersRepository,
    private val groupActionEventService: GroupActionEventService,
    private val groupPermissionService: GroupPermissionService,
) : GroupService {
    override fun findAllGroups(userId: UUID): Mono<List<GroupWithRole>> = groupRepository.findAllByUserIdOrderByName(userId).collectList()

    override fun findGroup(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole> =
        groupRepository.findOneByUserIdAndId(
            userId = userId,
            id = id,
        )

    @Transactional
    override fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): Mono<Group> =
        groupRepository
            .save(
                Group(
                    name = newGroupRequest.name,
                ),
            ).flatMap { g ->
                groupUserRepository
                    .save(
                        GroupUser(
                            userId = userId,
                            groupId = g.id!!,
                            role = UserGroupRole.ADMIN,
                        ),
                    ).thenReturn(g)
            }.flatMap { g ->
                groupActionEventService
                    .sendInsertedGroup(
                        group = g,
                        userId = userId,
                    ).thenReturn(g)
            }

    override fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): Mono<List<GroupUser>> =
        groupPermissionService.hasPermission(userId = userId, groupId = id).flatMap {
            if (it) {
                groupUserRepository
                    .findAllMembers(
                        id,
                    ).collectList()
            } else {
                Mono.empty()
            }
        }

    override fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Mono<Boolean> {
        require(memberId != id) { "User cannot changed his own role." }

        return groupPermissionService.hasPermission(userId = userId, groupId = id, roleNeeded = UserGroupRole.ADMIN).flatMap {
            if (it) {
                groupUserRepository
                    .updateRole(
                        userId = memberId,
                        groupId = id,
                        newRole = newRole,
                    ).map { count -> count > 0 }
            } else {
                Mono.empty()
            }
        }
    }
}
