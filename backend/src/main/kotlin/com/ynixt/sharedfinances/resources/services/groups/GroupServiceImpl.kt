package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.GroupOwnerCannotLeaveException
import com.ynixt.sharedfinances.domain.exceptions.http.GroupOwnerRequiredException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupOwnershipTransferException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupOwnershipTransferException.Reason
import com.ynixt.sharedfinances.domain.exceptions.http.MemberAlreadyInGroupException
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalContributionScheduleSpringDataRepository
import com.ynixt.sharedfinances.resources.services.EntityServiceImpl
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val recurrenceEventRepository: RecurrenceEventRepository,
    private val goalContributionScheduleRepository: FinancialGoalContributionScheduleSpringDataRepository,
    private val planQuotaService: PlanQuotaService,
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

    override suspend fun searchGroups(
        userId: UUID,
        pageable: Pageable,
        query: String?,
    ): Page<GroupWithRole> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }

        return if (normalizedQuery == null) {
            createPage(
                pageable,
                countFn = { repository.findAllByUserIdOrderByName(userId).count() },
            ) {
                repository
                    .findAllByUserIdOrderByName(userId)
                    .skip(pageable.offset)
                    .take(pageable.pageSize.toLong())
            }
        } else {
            createPage(
                pageable,
                countFn = {
                    repository.countByUserIdAndNameContainingIgnoreCase(
                        userId = userId,
                        name = normalizedQuery,
                    )
                },
            ) {
                repository
                    .searchByUserIdAndNameContainingIgnoreCase(
                        userId = userId,
                        name = normalizedQuery,
                    ).skip(pageable.offset)
                    .take(pageable.pageSize.toLong())
            }
        }.map { groupWithRole ->
            groupWithRole.apply {
                permissions = groupPermissionService.getAllPermissionsForRole(groupWithRole.role)
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
                            groupActionEventService.sendUpdatedGroup(
                                groupId = id,
                                name = request.name,
                                userId = userId,
                            )
                            findGroup(
                                userId = userId,
                                id = id,
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
    ): Boolean {
        val group = repository.findOneByUserIdAndId(userId, id).awaitSingleOrNull() ?: return false
        if (!group.isOwner) {
            throw GroupOwnerRequiredException()
        }

        val memberList =
            groupUserRepository
                .findAllMembers(id)
                .map { it.userId }
                .collectList()
                .awaitSingle()
        val modifiedLines = repository.deleteById(id).awaitSingle()
        if (modifiedLines > 0) {
            groupActionEventService.sendDeletedGroup(
                id = id,
                userId = userId,
                membersId = memberList,
            )
            planQuotaService.usageChanged(userId, PlanLimitKey.OWNED_GROUPS)
        }
        return modifiedLines > 0
    }

    @Transactional
    override suspend fun transferOwnership(
        userId: UUID,
        groupId: UUID,
        newOwnerId: UUID,
    ): GroupWithRole? {
        val group = repository.findOneByUserIdAndId(userId, groupId).awaitSingleOrNull() ?: return null
        if (!group.isOwner) {
            throw GroupOwnerRequiredException()
        }
        if (newOwnerId == userId) {
            throw InvalidGroupOwnershipTransferException(Reason.SELF_TRANSFER)
        }

        val newOwnerMembership =
            groupUserRepository.findOneByGroupIdAndUserId(groupId, newOwnerId).awaitSingleOrNull()
                ?: throw InvalidGroupOwnershipTransferException(Reason.TARGET_NOT_MEMBER)

        planQuotaService.assertCanAdd(
            quotaOwnerUserId = newOwnerId,
            quota = PlanLimitKey.OWNED_GROUPS,
            requesterUserId = userId,
        )

        if (newOwnerMembership.role != UserGroupRole.ADMIN) {
            groupUserRepository.updateRole(newOwnerId, groupId, UserGroupRole.ADMIN).awaitSingle()
        }
        repository.updateOwnerUserId(groupId, newOwnerId).awaitSingle()

        planQuotaService.usageChanged(userId, PlanLimitKey.OWNED_GROUPS)
        planQuotaService.usageChanged(newOwnerId, PlanLimitKey.OWNED_GROUPS)

        groupActionEventService.sendOwnershipChanged(
            userId = userId,
            groupId = groupId,
            previousOwnerUserId = userId,
            newOwnerUserId = newOwnerId,
        )

        return findGroup(userId, groupId)
    }

    @Transactional
    override suspend fun leaveGroup(
        userId: UUID,
        groupId: UUID,
    ): Boolean {
        val group = repository.findOneByUserIdAndId(userId, groupId).awaitSingleOrNull() ?: return false
        if (group.isOwner) {
            throw GroupOwnerCannotLeaveException()
        }

        val membersId =
            groupUserRepository
                .findAllMembers(groupId)
                .map { it.userId }
                .collectList()
                .awaitSingle()
        val departingItems = groupWalletItemRepository.findAllAssociatedOwnedByUser(groupId, userId).collectList().awaitSingle()
        val walletItemIds = departingItems.mapNotNull { it.id }

        if (walletItemIds.isNotEmpty()) {
            recurrenceEventRepository.endAllByGroupIdAndWalletItemIds(groupId, walletItemIds).awaitSingle()
            goalContributionScheduleRepository
                .endAllByGroupIdAndWalletItemIds(groupId, walletItemIds.toTypedArray())
                .awaitSingle()
            walletItemIds.forEach { walletItemId ->
                groupWalletItemRepository.deleteByGroupIdAndWalletItemId(groupId, walletItemId).awaitSingle()
            }
        }

        val deleted = groupUserRepository.deleteByGroupIdAndUserId(groupId, userId).awaitSingle() > 0
        if (!deleted) return false

        departingItems.forEach { item ->
            when (item.type) {
                WalletItemType.BANK_ACCOUNT ->
                    groupActionEventService.sendBankUnassociated(userId, groupId, item.id!!)
                WalletItemType.CREDIT_CARD ->
                    groupActionEventService.sendCreditCardUnassociated(userId, groupId, item.id!!)
            }
        }
        groupActionEventService.sendMemberLeft(
            userId = userId,
            groupId = groupId,
            departedUserId = userId,
            membersId = membersId,
        )
        planQuotaService.groupUsageChanged(groupId, PlanLimitKey.GROUP_MEMBERS, userId)
        planQuotaService.groupUsageChanged(groupId, PlanLimitKey.GROUP_ACTIVE_SCHEDULES, userId)
        return true
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

                group
                    .copy(
                        itemsAssociated = associatedBanks + associatedCreditCards,
                    ).also {
                        it.permissions = group.permissions
                    }
            }

    @Transactional
    override suspend fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): GroupEntity =
        planQuotaService
            .assertCanAdd(userId, PlanLimitKey.OWNED_GROUPS)
            .let {
                repository
                    .save(
                        GroupEntity(
                            name = newGroupRequest.name,
                            ownerUserId = userId,
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
                                    userId = userId,
                                    groupId = group.id!!,
                                    categories = newGroupRequest.categories,
                                )
                        }

                        groupCategoryService.ensureDebtSfCategory(group.id!!)

                        groupActionEventService
                            .sendInsertedGroup(
                                group = group,
                                userId = userId,
                            )

                        planQuotaService.usageChanged(userId, PlanLimitKey.OWNED_GROUPS)

                        group
                    }
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
        return groupPermissionService
            .hasPermission(userId = userId, groupId = id, permission = GroupPermissions.CHANGE_ROLE)
            .let {
                if (it) {
                    val group = repository.findById(id).awaitSingleOrNull() ?: return@let false
                    // Ownership is also membership: future leave/remove-member paths must reject removing the owner.
                    if (memberId == group.ownerUserId && newRole != UserGroupRole.ADMIN) {
                        throw InvalidGroupOwnershipTransferException(Reason.OWNER_MUST_BE_ADMIN)
                    }
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

    override suspend fun updateOwnPlanningSimulatorOptIn(
        userId: UUID,
        id: UUID,
        allowPlanningSimulator: Boolean,
    ): Boolean {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = id)) {
            return false
        }

        return groupUserRepository
            .updatePlanningSimulatorOptIn(
                userId = userId,
                groupId = id,
                allowPlanningSimulator = allowPlanningSimulator,
            ).awaitSingle() > 0
    }
}
