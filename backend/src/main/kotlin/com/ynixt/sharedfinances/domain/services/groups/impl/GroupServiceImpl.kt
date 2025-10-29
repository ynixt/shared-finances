package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.entities.groups.Group
import com.ynixt.sharedfinances.domain.entities.groups.GroupUser
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.exceptions.MemberAlreadyInGroupException
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
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
    private val databaseHelperService: DatabaseHelperService,
    private val groupCategoryService: GroupCategoryService,
) : GroupService {
    override fun findAllGroups(userId: UUID): Mono<List<GroupWithRole>> =
        groupRepository.findAllByUserIdOrderByName(userId).collectList().map { list ->
            list.map { groupWithRole ->
                groupWithRole.apply {
                    this.permissions = groupPermissionService.getAllPermissionsForRole(groupWithRole.role)
                }
            }
        }

    @Transactional
    override fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): Mono<GroupWithRole> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = id,
                GroupPermissions.EDIT_GROUP,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupRepository
                        .edit(id, request.name)
                        .flatMap {
                            findGroup(
                                userId = userId,
                                id = id,
                            )
                        }.flatMap { g ->
                            groupActionEventService
                                .sendUpdatedGroup(
                                    group = g,
                                    userId = userId,
                                ).thenReturn(g)
                        }
                } else {
                    Mono.empty()
                }
            }

    @Transactional
    override fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = id,
                GroupPermissions.EDIT_GROUP,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupUserRepository.findAllMembers(id).map { it.userId }.collectList().flatMap { memberList ->
                        groupRepository.deleteById(id).flatMap { modifiedLines ->
                            if (modifiedLines > 0) {
                                groupActionEventService
                                    .sendDeletedGroup(
                                        id = id,
                                        userId = userId,
                                        membersId = memberList.toList(),
                                    ).thenReturn(true)
                            } else {
                                Mono.just(false)
                            }
                        }
                    }
                } else {
                    Mono.empty()
                }
            }.switchIfEmpty(Mono.just(false))

    override fun findGroup(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole> =
        groupRepository
            .findOneByUserIdAndId(
                userId = userId,
                id = id,
            ).map { groupWithRole ->
                groupWithRole.apply {
                    this.permissions = groupPermissionService.getAllPermissionsForRole(groupWithRole.role)
                }
            }

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
                if (newGroupRequest.categories != null) {
                    groupCategoryService
                        .newCategories(
                            groupId = g.id!!,
                            categories = newGroupRequest.categories,
                        ).thenReturn(g)
                } else {
                    Mono.just(g)
                }
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

        return groupPermissionService.hasPermission(userId = userId, groupId = id, permission = GroupPermissions.CHANGE_ROLE).flatMap {
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

    override fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole,
    ): Mono<Unit> =
        groupUserRepository
            .save(
                GroupUser(
                    userId = userId,
                    groupId = id,
                    role = role,
                ),
            ).onErrorMap { t ->
                if (databaseHelperService.isUniqueViolation(t, "idx_group_user_group_id_user_id")) {
                    MemberAlreadyInGroupException(
                        userId = userId,
                        groupId = id,
                        cause = t,
                    )
                } else {
                    t
                }
            }.map { }
}
