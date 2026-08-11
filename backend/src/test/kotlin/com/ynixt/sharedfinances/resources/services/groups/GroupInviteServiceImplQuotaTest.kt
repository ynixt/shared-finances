package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupInviteEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.exceptions.http.PlanQuotaExceededException
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class GroupInviteServiceImplQuotaTest {
    @Test
    fun `issuing counts outstanding invitations and refuses before saving`() =
        runTest {
            val repository = Invitations()
            val quota = RecordingQuota(fail = true)
            val service = service(repository, quota)
            val groupId = UUID.randomUUID()

            assertThrows<PlanQuotaExceededException> { service.generate(UUID.randomUUID(), groupId) }
            assertEquals(true, quota.includeOutstandingInvitations)
            assertEquals(0, repository.saves)
        }

    @Test
    fun `acceptance consumes the invitation then counts members only before adding`() =
        runTest {
            val repository = Invitations()
            val quota = RecordingQuota()
            val groupService = Mockito.mock(GroupService::class.java)
            val service = service(repository, quota, groupService)
            val userId = UUID.randomUUID()

            assertEquals(repository.invite.groupId, service.accept(userId, repository.invite.id!!))
            assertEquals(false, quota.includeOutstandingInvitations)
            assertEquals(listOf("delete", "quota"), repository.order + quota.order)
            Mockito.verify(groupService).addNewMember(userId, repository.invite.groupId)
        }

    @Test
    fun `acceptance consumes an invitation but does not add when the group filled meanwhile`() =
        runTest {
            val repository = Invitations()
            val quota = RecordingQuota(fail = true)
            val groupService = Mockito.mock(GroupService::class.java)
            val service = service(repository, quota, groupService)

            assertThrows<PlanQuotaExceededException> { service.accept(UUID.randomUUID(), repository.invite.id!!) }

            assertEquals(false, quota.includeOutstandingInvitations)
            assertEquals(listOf("delete", "quota"), repository.order + quota.order)
            Mockito.verifyNoInteractions(groupService)
        }

    private fun service(
        repository: Invitations,
        quota: RecordingQuota,
        groupService: GroupService = Mockito.mock(GroupService::class.java),
    ) = GroupInviteServiceImpl(repository, Allow, groupService, 60, quota)

    private object Allow : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ) = true

        override fun getAllPermissionsForRole(role: UserGroupRole) = emptySet<GroupPermissions>()
    }

    private class Invitations : GroupInviteRepository {
        val invite = GroupInviteEntity(UUID.randomUUID(), OffsetDateTime.now().plusHours(1)).also { it.id = UUID.randomUUID() }
        var saves = 0
        val order = mutableListOf<String>()

        override fun findById(id: UUID) = Mono.just(invite)

        override fun save(invite: GroupInviteEntity) = Mono.just(invite).also { saves++ }

        override fun deleteAllByExpireAtLessThanEqual(expireAt: OffsetDateTime) = Mono.just(0L)

        override fun findInfoForInvite(inviteId: UUID) = Mono.empty<GroupInfoForInvite>()

        override fun deleteOneByIdAndExpireAtGreaterThan(
            id: UUID,
            expireAt: OffsetDateTime,
        ) = Mono.just(1L).also { order += "delete" }
    }

    private class RecordingQuota(
        private val fail: Boolean = false,
    ) : PlanQuotaService {
        var includeOutstandingInvitations: Boolean? = null
        val order = mutableListOf<String>()

        override suspend fun assertCanAdd(
            quotaOwnerUserId: UUID,
            quota: PlanLimitKey,
            requesterUserId: UUID,
        ) = Unit

        override suspend fun currentUsage(
            userId: UUID,
            quota: PlanLimitKey,
        ) = 0L

        override suspend fun usageChanged(
            userId: UUID,
            quota: PlanLimitKey,
        ) = Unit

        override suspend fun assertGroupCanAdd(
            groupId: UUID,
            quota: PlanLimitKey,
            requesterUserId: UUID,
            includeOutstandingInvitations: Boolean,
        ) {
            this.includeOutstandingInvitations = includeOutstandingInvitations
            order += "quota"
            if (fail) throw PlanQuotaExceededException(quota, groupId = groupId)
        }
    }
}
