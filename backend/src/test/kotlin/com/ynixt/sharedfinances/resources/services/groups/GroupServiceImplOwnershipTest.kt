package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.exceptions.http.GroupOwnerCannotLeaveException
import com.ynixt.sharedfinances.domain.exceptions.http.GroupOwnerRequiredException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupOwnershipTransferException
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.categories.GroupCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.FinancialGoalContributionScheduleSpringDataRepository
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.InMemoryAccountDeletionGroupStore
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupWalletItemRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class GroupServiceImplOwnershipTest {
    private val store = InMemoryAccountDeletionGroupStore()
    private val events = RecordingGroupEvents()
    private val groupCategoryService = Mockito.mock(GroupCategoryService::class.java)
    private val service =
        GroupServiceImpl(
            repository = store,
            groupUserRepository = store,
            groupActionEventService = events,
            groupPermissionService = StoreBackedPermissions(store),
            databaseHelperService = Mockito.mock(DatabaseHelperService::class.java),
            groupCategoryService = groupCategoryService,
            groupBankAssociationService = Mockito.mock(GroupBankAssociationService::class.java),
            creditCardAssociationService = Mockito.mock(GroupCreditCardAssociationService::class.java),
            groupWalletItemRepository = NoOpGroupWalletItemRepository(),
            recurrenceEventRepository = Mockito.mock(RecurrenceEventRepository::class.java),
            goalContributionScheduleRepository = Mockito.mock(FinancialGoalContributionScheduleSpringDataRepository::class.java),
        )

    @Test
    fun `new group is owned by its creator who is an admin`() =
        runTest {
            val creatorId = UUID.randomUUID()
            val group = service.newGroup(creatorId, NewGroupRequest("Home", null))

            assertThat(group.ownerUserId).isEqualTo(creatorId)
            assertThat(store.findOneByGroupIdAndUserId(group.id!!, creatorId).awaitSingle().role).isEqualTo(UserGroupRole.ADMIN)
        }

    @Test
    fun `only the owner can delete while non-members still receive not found semantics`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val adminId = UUID.randomUUID()
            val groupId = group(ownerId, adminId to UserGroupRole.ADMIN)

            assertThatThrownBy { kotlinx.coroutines.runBlocking { service.deleteGroup(adminId, groupId) } }
                .isInstanceOf(GroupOwnerRequiredException::class.java)
            assertThat(service.deleteGroup(UUID.randomUUID(), groupId)).isFalse()
            assertThat(service.deleteGroup(ownerId, groupId)).isTrue()
            assertThat(store.existsById(groupId).awaitSingle()).isFalse()
            assertThat(events.deletedGroupIds).containsExactly(groupId)
        }

    @Test
    fun `transfer to an admin changes owner and keeps the previous owner admin`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val destinationId = UUID.randomUUID()
            val groupId = group(ownerId, destinationId to UserGroupRole.ADMIN)

            val response = service.transferOwnership(ownerId, groupId, destinationId)

            assertThat(response!!.ownerUserId).isEqualTo(destinationId)
            assertThat(response.isOwner).isFalse()
            assertThat(store.findById(groupId).awaitSingle().ownerUserId).isEqualTo(destinationId)
            assertThat(store.findOneByGroupIdAndUserId(groupId, ownerId).awaitSingle().role).isEqualTo(UserGroupRole.ADMIN)
            assertThat(events.updatedGroupIds).containsExactly(groupId)
        }

    @Test
    fun `transfer to editor or viewer promotes the destination to admin`() =
        runTest {
            for (role in listOf(UserGroupRole.EDITOR, UserGroupRole.VIEWER)) {
                val ownerId = UUID.randomUUID()
                val destinationId = UUID.randomUUID()
                val groupId = group(ownerId, destinationId to role)

                service.transferOwnership(ownerId, groupId, destinationId)

                assertThat(store.findOneByGroupIdAndUserId(groupId, destinationId).awaitSingle().role).isEqualTo(UserGroupRole.ADMIN)
                assertThat(store.findById(groupId).awaitSingle().ownerUserId).isEqualTo(destinationId)
            }
        }

    @Test
    fun `invalid transfers preserve ownership`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val adminId = UUID.randomUUID()
            val groupId = group(ownerId, adminId to UserGroupRole.ADMIN)

            assertThatThrownBy { kotlinx.coroutines.runBlocking { service.transferOwnership(ownerId, groupId, UUID.randomUUID()) } }
                .isInstanceOf(InvalidGroupOwnershipTransferException::class.java)
            assertThatThrownBy { kotlinx.coroutines.runBlocking { service.transferOwnership(ownerId, groupId, ownerId) } }
                .isInstanceOf(InvalidGroupOwnershipTransferException::class.java)
            assertThatThrownBy { kotlinx.coroutines.runBlocking { service.transferOwnership(adminId, groupId, ownerId) } }
                .isInstanceOf(GroupOwnerRequiredException::class.java)
            assertThat(store.findById(groupId).awaitSingle().ownerUserId).isEqualTo(ownerId)
        }

    @Test
    fun `neither another admin nor the owner can downgrade the owner`() =
        runTest {
            val ownerId = UUID.randomUUID()
            val adminId = UUID.randomUUID()
            val groupId = group(ownerId, adminId to UserGroupRole.ADMIN)

            assertThatThrownBy {
                kotlinx.coroutines.runBlocking { service.updateMemberRole(adminId, groupId, ownerId, UserGroupRole.EDITOR) }
            }.isInstanceOf(InvalidGroupOwnershipTransferException::class.java)
            assertThatThrownBy {
                kotlinx.coroutines.runBlocking { service.updateMemberRole(ownerId, groupId, ownerId, UserGroupRole.VIEWER) }
            }.isInstanceOf(InvalidGroupOwnershipTransferException::class.java)
            assertThat(store.findOneByGroupIdAndUserId(groupId, ownerId).awaitSingle().role).isEqualTo(UserGroupRole.ADMIN)
        }

    @Test
    fun `admin editor and viewer can leave while owner and non-member cannot`() =
        runTest {
            for (role in listOf(UserGroupRole.ADMIN, UserGroupRole.EDITOR, UserGroupRole.VIEWER)) {
                val ownerId = UUID.randomUUID()
                val memberId = UUID.randomUUID()
                val groupId = group(ownerId, memberId to role)

                assertThat(service.leaveGroup(memberId, groupId)).isTrue()
                assertThat(store.findOneByGroupIdAndUserId(groupId, memberId).awaitSingleOrNull()).isNull()
                assertThat(store.existsById(groupId).awaitSingle()).isTrue()
            }

            val ownerId = UUID.randomUUID()
            val groupId = group(ownerId)
            assertThatThrownBy { kotlinx.coroutines.runBlocking { service.leaveGroup(ownerId, groupId) } }
                .isInstanceOf(GroupOwnerCannotLeaveException::class.java)
            assertThat(service.leaveGroup(UUID.randomUUID(), groupId)).isFalse()
        }

    private suspend fun group(
        ownerId: UUID,
        vararg others: Pair<UUID, UserGroupRole>,
    ): UUID {
        val group = store.save(GroupEntity("Shared", ownerId)).awaitSingle()
        store.save(GroupUserEntity(group.id!!, ownerId, UserGroupRole.ADMIN)).awaitSingle()
        others.forEach { (userId, role) -> store.save(GroupUserEntity(group.id!!, userId, role)).awaitSingle() }
        return group.id!!
    }

    private class StoreBackedPermissions(
        private val store: InMemoryAccountDeletionGroupStore,
    ) : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean {
            val membership = store.findOneByGroupIdAndUserId(groupId, userId).awaitSingleOrNull() ?: return false
            return permission == null || permission in getAllPermissionsForRole(membership.role)
        }

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> =
            if (role == UserGroupRole.ADMIN) GroupPermissions.entries.toSet() else emptySet()
    }

    private class RecordingGroupEvents : GroupActionEventService {
        val updatedGroupIds = mutableListOf<UUID>()
        val deletedGroupIds = mutableListOf<UUID>()
        val departedUserIds = mutableListOf<UUID>()

        override suspend fun sendInsertedGroup(
            userId: UUID,
            group: GroupEntity,
        ) {}

        override suspend fun sendUpdatedGroup(
            userId: UUID,
            groupId: UUID,
            name: String,
        ) {
            updatedGroupIds += groupId
        }

        override suspend fun sendOwnershipChanged(
            userId: UUID,
            groupId: UUID,
            previousOwnerUserId: UUID,
            newOwnerUserId: UUID,
        ) {
            updatedGroupIds += groupId
        }

        override suspend fun sendMemberLeft(
            userId: UUID,
            groupId: UUID,
            departedUserId: UUID,
            membersId: List<UUID>,
        ) {
            departedUserIds += departedUserId
        }

        override suspend fun sendDeletedGroup(
            userId: UUID,
            id: UUID,
            membersId: List<UUID>,
        ) {
            deletedGroupIds += id
        }

        override suspend fun sendBankAssociated(
            userId: UUID,
            groupBankAccount: com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity,
        ) {
        }

        override suspend fun sendBankUnassociated(
            userId: UUID,
            groupId: UUID,
            bankAccountId: UUID,
        ) {}

        override suspend fun sendCreditCardAssociated(
            userId: UUID,
            groupCreditCard: com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity,
        ) {
        }

        override suspend fun sendCreditCardUnassociated(
            userId: UUID,
            groupId: UUID,
            creditCardId: UUID,
        ) {}
    }
}
