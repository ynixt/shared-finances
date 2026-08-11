package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.plan.GroupPlanTierService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupEntitlementsServiceImplTest {
    @Test
    fun `member receives common and pro payloads while non-member receives nothing`() =
        runTest {
            val access = Access(true)
            val tiers = Tiers(GroupPlanTier.COMMON)
            val service = GroupEntitlementsServiceImpl(access, tiers, Limits, Usage, PlanProperties(enabled = true))
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()

            val common = service.get(userId, groupId)!!
            assertEquals(GroupPlanTier.COMMON, common.tier)
            assertTrue(common.limitsEnabled)
            assertEquals(4, common.quotas.size)
            assertTrue(common.quotas.all { it.limit == 4 && it.usage == 2L && !it.unlimited })

            tiers.tier = GroupPlanTier.PRO
            assertEquals(GroupPlanTier.PRO, service.get(userId, groupId)!!.tier)

            access.allowed = false
            assertNull(service.get(userId, groupId))
        }

    @Test
    fun `every group role can read the same entitlement payload`() =
        runTest {
            val members = UserGroupRole.entries.associateWith { UUID.randomUUID() }
            val access = RoleAccess(members.entries.associate { (role, userId) -> userId to role })
            val service = GroupEntitlementsServiceImpl(access, Tiers(GroupPlanTier.COMMON), Limits, Usage, PlanProperties(enabled = true))
            val groupId = UUID.randomUUID()

            members.forEach { (_, userId) ->
                val payload = assertNotNull(service.get(userId, groupId))
                assertEquals(GroupPlanTier.COMMON, payload.tier)
                assertEquals(4, payload.quotas.size)
            }
            assertNull(service.get(UUID.randomUUID(), groupId))
        }

    @Test
    fun `group payload reports disabled plan model explicitly`() =
        runTest {
            val service = GroupEntitlementsServiceImpl(Access(true), Tiers(GroupPlanTier.COMMON), Limits, Usage, PlanProperties())

            assertTrue(!service.get(UUID.randomUUID(), UUID.randomUUID())!!.limitsEnabled)
        }

    private class Access(
        var allowed: Boolean,
    ) : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ) = allowed

        override fun getAllPermissionsForRole(role: UserGroupRole) = emptySet<GroupPermissions>()
    }

    private class RoleAccess(
        private val members: Map<UUID, UserGroupRole>,
    ) : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ) = members.containsKey(userId)

        override fun getAllPermissionsForRole(role: UserGroupRole) = emptySet<GroupPermissions>()
    }

    private class Tiers(
        var tier: GroupPlanTier,
    ) : GroupPlanTierService {
        override suspend fun resolve(groupId: UUID) = tier
    }

    private object Limits : PlanLimitService {
        override suspend fun resolve(
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = ResolvedPlanLimit.unlimited()

        override suspend fun resolve(
            tier: GroupPlanTier,
            quota: PlanLimitKey,
        ) = ResolvedPlanLimit.finite(4)

        override suspend fun save(limit: PlanLimitEntity) = limit

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) = Unit
    }

    private object Usage : PlanQuotaService {
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

        override suspend fun currentGroupUsage(
            groupId: UUID,
            quota: PlanLimitKey,
            includeOutstandingInvitations: Boolean,
        ) = 2L
    }
}
