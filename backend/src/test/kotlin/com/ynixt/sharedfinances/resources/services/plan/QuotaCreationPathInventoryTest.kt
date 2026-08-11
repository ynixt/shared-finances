package com.ynixt.sharedfinances.resources.services.plan

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuotaCreationPathInventoryTest {
    private val services = Path.of("src/main/kotlin/com/ynixt/sharedfinances/resources/services")

    @Test
    fun `every personal creation path is guarded by its matching quota`() {
        assertContains("BankAccountServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.BANK_ACCOUNTS)")
        assertContains("CreditCardServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.CREDIT_CARDS)")
        assertContains("categories/UserCategoryServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.CATEGORIES)")
        assertContains("goals/FinancialGoalManagementServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.GOALS)")
        assertContains("imports/ImportServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.IMPORTS_PER_MONTH)")
        assertContains("simulation/SimulationJobServiceImpl.kt", "assertCanAdd(ownerUserId, PlanLimitKey.SIMULATIONS_PER_MONTH)")
        assertContains("simulation/SimulationJobServiceImpl.kt", "assertCanAdd(requesterUserId, PlanLimitKey.SIMULATIONS_PER_MONTH)")
        assertContains("groups/GroupServiceImpl.kt", "assertCanAdd(userId, PlanLimitKey.OWNED_GROUPS)")
        assertContains("groups/GroupServiceImpl.kt", "quotaOwnerUserId = newOwnerId")
        assertContains(
            "walletentry/WalletEntrySaveServiceImpl.kt",
            "planQuotaService.assertCanAdd(userId, PlanLimitKey.ACTIVE_SCHEDULES)",
        )
    }

    @Test
    fun `every group creation path is guarded by its matching group quota`() {
        assertContains("categories/GroupCategoryServiceImpl.kt", "assertGroupCanAdd(groupId, PlanLimitKey.GROUP_CATEGORIES")
        assertContains("goals/FinancialGoalManagementServiceImpl.kt", "assertGroupCanAdd(groupId, PlanLimitKey.GROUP_GOALS")
        assertContains("walletentry/WalletEntrySaveServiceImpl.kt", "PlanLimitKey.GROUP_ACTIVE_SCHEDULES")
        assertContains("groups/GroupInviteServiceImpl.kt", "includeOutstandingInvitations = true")
        assertContains("groups/GroupInviteServiceImpl.kt", "assertGroupCanAdd(invite.groupId, PlanLimitKey.GROUP_MEMBERS")

        val groupService = source("groups/GroupServiceImpl.kt")
        assertEquals(2, Regex("GroupUserEntity\\(").findAll(groupService).count())
        assertTrue(groupService.contains("newGroupRequest"), "The owner membership is created with the group")
        assertTrue(groupService.contains("override suspend fun addNewMember"), "Invitation acceptance is the only later membership path")
    }

    @Test
    fun `personal recurrence persistence cannot bypass the quota chokepoint`() {
        val recurrenceSavers =
            Files.walk(services).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                    .filter { Regex("recurrenceEventRepository\\s*\\.save").containsMatchIn(it.readText()) }
                    .map { services.relativize(it).toString().replace('\\', '/') }
                    .toList()
            }

        assertEquals(
            setOf(
                "walletentry/WalletEntryMutationSupportServiceImpl.kt",
                "walletentry/WalletEntrySaveServiceImpl.kt",
            ),
            recurrenceSavers.toSet(),
            "A new recurrence persistence path must be routed through the quota chokepoint or explicitly reviewed here",
        )

        val guardedCreation = source("walletentry/WalletEntrySaveServiceImpl.kt")
        assertTrue(guardedCreation.contains("id == null && recurrenceToPersist.nextExecution != null"))
        assertTrue(guardedCreation.contains("recurrenceToPersist.nextExecution != null"))
        assertTrue(guardedCreation.contains("assertCanAdd(userId, PlanLimitKey.ACTIVE_SCHEDULES)"))

        val allowlistedInPlaceUpdate = source("walletentry/WalletEntryMutationSupportServiceImpl.kt")
        assertTrue(allowlistedInPlaceUpdate.contains("it.id = current.id"))
    }

    @Test
    fun `system categories bypass quotas while group goals use the group quota`() {
        val categories = source("categories/UserCategoryServiceImpl.kt")
        val ensureDebtBody = categories.substringAfter("override suspend fun ensureDebtSfCategory").substringBefore("override suspend fun")
        assertFalse(ensureDebtBody.contains("assertCanAdd"))

        val goals = source("goals/FinancialGoalManagementServiceImpl.kt")
        val groupGoalBody = goals.substringAfter("override suspend fun createGoalForGroup").substringBefore("override suspend fun")
        assertTrue(groupGoalBody.contains("assertGroupCanAdd(groupId, PlanLimitKey.GROUP_GOALS, userId)"))
    }

    @Test
    fun `usage repository defines exactly one indexed count for every quota`() {
        val repository = source("../repositories/r2dbc/databaseclient/PlanQuotaUsageDatabaseClientRepository.kt")
        assertEquals(12, Regex("PlanLimitKey\\.[A-Z_]+ ->").findAll(repository).count())
        PlanLimitKeyNames.values.forEach { assertTrue(repository.contains("PlanLimitKey.$it"), "Missing count for $it") }
    }

    @Test
    fun `ended and group schedules are excluded from personal active schedule usage`() {
        val repository = source("../repositories/r2dbc/databaseclient/PlanQuotaUsageDatabaseClientRepository.kt")
        val scheduleCount =
            repository
                .substringAfter("PlanLimitKey.ACTIVE_SCHEDULES ->")
                .substringBefore("PlanLimitKey.IMPORTS_PER_MONTH ->")

        assertTrue(scheduleCount.contains("group_id IS NULL"))
        assertTrue(scheduleCount.contains("next_execution IS NOT NULL"))
    }

    @Test
    fun `queuing monthly work emits no usage delta before successful completion`() {
        val imports = source("imports/ImportServiceImpl.kt")
        val importCreate = imports.substringAfter("override suspend fun create(").substringBefore("override suspend fun list(")
        assertFalse(importCreate.contains("usageChanged"))

        val simulations = source("simulation/SimulationJobServiceImpl.kt")
        val simulationCreates =
            simulations
                .substringAfter(
                    "override suspend fun create(",
                ).substringBefore("override suspend fun getForOwner(")
        assertFalse(simulationCreates.contains("usageChanged"))
    }

    private fun assertContains(
        relative: String,
        text: String,
    ) = assertTrue(source(relative).contains(text), "$relative does not contain $text")

    private fun source(relative: String) = services.resolve(relative).normalize().readText()

    private object PlanLimitKeyNames {
        val values =
            listOf(
                "BANK_ACCOUNTS",
                "CREDIT_CARDS",
                "CATEGORIES",
                "GOALS",
                "ACTIVE_SCHEDULES",
                "IMPORTS_PER_MONTH",
                "SIMULATIONS_PER_MONTH",
                "OWNED_GROUPS",
                "GROUP_CATEGORIES",
                "GROUP_GOALS",
                "GROUP_ACTIVE_SCHEDULES",
                "GROUP_MEMBERS",
            )
    }
}
