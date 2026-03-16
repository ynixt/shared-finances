package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.exceptions.http.MemberAlreadyInGroupException
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.resources.services.EntityServiceImpl
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GroupServiceImpl(
    override val repository: GroupRepository,
    private val groupUserRepository: GroupUsersRepository,
    private val groupActionEventService: GroupActionEventService,
    private val groupPermissionService: GroupPermissionService,
    private val databaseHelperService: DatabaseHelperService,
    private val groupCategoryService: GroupCategoryService,
    private val groupBankAssociationService: GroupBankAssociationService,
    private val creditCardAssociationService: GroupCreditCardAssociationService,
) : EntityServiceImpl<GroupEntity, GroupEntity>(),
    GroupService {
    override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> =
        repository.findAllByUserIdOrderByName(userId).collectList().awaitSingle().let { list ->
            list.map { groupWithRole ->
                groupWithRole.apply {
                    this.permissions = groupPermissionService.getAllPermissionsForRole(groupWithRole.role)
                }
            }
        }

    @Transactional
    override suspend fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): GroupWithRole? =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = id,
                GroupPermissions.EDIT_GROUP,
            ).let { hasPermission ->
                if (hasPermission) {
                    repository
                        .edit(id, request.name)
                        .awaitSingle()
                        .let {
                            findGroup(
                                userId = userId,
                                id = id,
                            )
                        }?.also { g ->
                            groupActionEventService
                                .sendUpdatedGroup(
                                    group = g,
                                    userId = userId,
                                )
                        }
                } else {
                    null
                }
            }

    @Transactional
    override suspend fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = id,
                GroupPermissions.EDIT_GROUP,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupUserRepository.findAllMembers(id).map { it.userId }.collectList().awaitSingle().let { memberList ->
                        repository.deleteById(id).awaitSingle().also { modifiedLines ->
                            if (modifiedLines > 0) {
                                groupActionEventService
                                    .sendDeletedGroup(
                                        id = id,
                                        userId = userId,
                                        membersId = memberList.toList(),
                                    )
                            }

                            modifiedLines > 0
                        }
                    }
                }

                hasPermission
            }

    override suspend fun findGroup(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? =
        repository
            .findOneByUserIdAndId(
                userId = userId,
                id = id,
            ).awaitSingleOrNull()
            ?.let { groupWithRole ->
                groupWithRole.apply {
                    this.permissions = groupPermissionService.getAllPermissionsForRole(groupWithRole.role)
                }
            }

    override suspend fun findGroupWithAssociatedItems(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? =
        findGroup(userId, id)
            ?.let { group ->
                val associatedBanks =
                    groupBankAssociationService.findAllAssociatedBanks(
                        userId = userId,
                        groupId = id,
                    )

                val associatedCreditCards =
                    creditCardAssociationService.findAllAssociatedCreditCards(
                        userId = userId,
                        groupId = id,
                    )

                group.copy(
                    itemsAssociated = associatedBanks + associatedCreditCards,
                )
            }

    @Transactional
    override suspend fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): GroupEntity =
        repository
            .save(
                GroupEntity(
                    name = newGroupRequest.name,
                ),
            ).awaitSingle()
            .let { group ->
                groupUserRepository
                    .save(
                        GroupUserEntity(
                            userId = userId,
                            groupId = group.id!!,
                            role = UserGroupRole.ADMIN,
                        ),
                    ).awaitSingle()

                if (newGroupRequest.categories != null) {
                    groupCategoryService
                        .newCategories(
                            groupId = group.id!!,
                            categories = newGroupRequest.categories,
                        )
                }

                groupActionEventService
                    .sendInsertedGroup(
                        group = group,
                        userId = userId,
                    )

                group
            }

    override suspend fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): List<GroupUserEntity> =
        groupPermissionService.hasPermission(userId = userId, groupId = id).let {
            if (it) {
                groupUserRepository
                    .findAllMembers(
                        id,
                    ).collectList()
                    .awaitSingle()
            } else {
                emptyList()
            }
        }

    override suspend fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Boolean {
        require(memberId != id) { "User cannot changed his own role." }

        return groupPermissionService
            .hasPermission(userId = userId, groupId = id, permission = GroupPermissions.CHANGE_ROLE)
            .let {
                if (it) {
                    groupUserRepository
                        .updateRole(
                            userId = memberId,
                            groupId = id,
                            newRole = newRole,
                        ).awaitSingle()
                        .let { count -> count > 0 }
                } else {
                    false
                }
            }
    }

    override suspend fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole,
    ) {
        try {
            groupUserRepository
                .save(
                    GroupUserEntity(
                        userId = userId,
                        groupId = id,
                        role = role,
                    ),
                ).awaitSingle()
        } catch (t: Throwable) {
            throw if (databaseHelperService.isUniqueViolation(t, "idx_group_user_group_id_user_id")) {
                MemberAlreadyInGroupException(
                    userId = userId,
                    groupId = id,
                    cause = t,
                )
            } else {
                t
            }
        }
    }
}
