package com.ynixt.sharedfinances.scenarios.accountdeletion.support

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.resources.services.AccountDeletionServiceImpl
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupWalletItemRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import java.time.LocalDate
import java.util.UUID

fun accountDeletionScenario(block: suspend AccountDeletionScenarioDsl.() -> Unit): AccountDeletionScenarioDsl =
    runBlocking {
        AccountDeletionScenarioDsl().apply { block() }
    }

class AccountDeletionScenarioDsl {
    internal val userRepository = InMemoryUserRepository()
    internal val groupStore = InMemoryAccountDeletionGroupStore()
    internal val walletItemRepository = InMemoryWalletItemRepository()
    internal val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
    internal val recurrenceEventRepository = InMemoryRecurrenceEventRepository(walletItemRepository)
    internal val simulationJobService = RecordingComplianceSimulationJobService()
    internal val avatarService = RecordingAvatarService()

    private val accountDeletionService =
        AccountDeletionServiceImpl(
            userRepository = userRepository,
            groupRepository = groupStore,
            groupUsersRepository = groupStore,
            groupActionEventService = NoOpGroupActionEventServiceStub,
            groupWalletItemRepository = NoOpGroupWalletItemRepository(),
            walletEventRepository = walletEventRepository,
            recurrenceEventRepository = recurrenceEventRepository,
            simulationJobService = simulationJobService,
            sessionRepository = NoOpSessionRepository,
            avatarService = avatarService,
        )

    val given = Given(this)
    val whenActions = When(this)
    val then = Then(this)

    suspend fun given(block: suspend Given.() -> Unit): AccountDeletionScenarioDsl =
        chain {
            given.block()
        }

    suspend fun `when`(block: suspend When.() -> Unit): AccountDeletionScenarioDsl =
        chain {
            whenActions.block()
        }

    suspend fun then(block: suspend Then.() -> Unit): AccountDeletionScenarioDsl =
        chain {
            then.block()
        }

    private suspend fun chain(action: suspend () -> Unit): AccountDeletionScenarioDsl {
        action()
        return this
    }

    class Given(
        private val dsl: AccountDeletionScenarioDsl,
    ) {
        suspend fun user(
            email: String = "user-${UUID.randomUUID()}@example.com",
            firstName: String = "Test",
            lastName: String = "User",
        ): UUID {
            val entity =
                UserEntity(
                    email = email,
                    passwordHash = "x",
                    firstName = firstName,
                    lastName = lastName,
                    lang = "pt-BR",
                    defaultCurrency = "BRL",
                    tmz = "America/Sao_Paulo",
                    photoUrl = null,
                    emailVerified = true,
                    mfaEnabled = false,
                    totpSecret = null,
                    onboardingDone = true,
                )
            return dsl.userRepository
                .insert(entity)
                .awaitSingle()
                .id!!
        }

        suspend fun group(
            name: String,
            vararg members: Pair<UUID, UserGroupRole>,
        ): UUID {
            val saved = dsl.groupStore.save(GroupEntity(name)).awaitSingle()
            val gid = saved.id!!
            for ((uid, role) in members) {
                dsl.groupStore.save(GroupUserEntity(gid, uid, role)).awaitSingle()
            }
            return gid
        }

        suspend fun groupScopedWalletEvent(
            userId: UUID,
            groupId: UUID,
            date: LocalDate = LocalDate.of(2026, 1, 15),
        ) {
            dsl.walletEventRepository
                .save(
                    WalletEventEntity(
                        type = WalletEntryType.EXPENSE,
                        name = "Coffee",
                        categoryId = null,
                        createdByUserId = userId,
                        groupId = groupId,
                        tags = null,
                        observations = null,
                        date = date,
                        confirmed = true,
                        installment = null,
                        recurrenceEventId = null,
                        paymentType = PaymentType.UNIQUE,
                    ),
                ).awaitSingle()
        }

        /** Personal ledger row (`group_id` null) — must be purged before `users` delete or DB CASCADE on `wallet_item` fails. */
        suspend fun personalWalletEvent(
            userId: UUID,
            date: LocalDate = LocalDate.of(2026, 1, 12),
        ) {
            dsl.walletEventRepository
                .save(
                    WalletEventEntity(
                        type = WalletEntryType.EXPENSE,
                        name = "Groceries",
                        categoryId = null,
                        createdByUserId = userId,
                        groupId = null,
                        tags = null,
                        observations = null,
                        date = date,
                        confirmed = true,
                        installment = null,
                        recurrenceEventId = null,
                        paymentType = PaymentType.UNIQUE,
                    ),
                ).awaitSingle()
        }

        suspend fun groupScopedRecurrence(
            userId: UUID,
            groupId: UUID,
        ) {
            dsl.recurrenceEventRepository
                .save(
                    RecurrenceEventEntity(
                        name = "Rent",
                        categoryId = null,
                        createdByUserId = userId,
                        groupId = groupId,
                        tags = null,
                        observations = null,
                        type = WalletEntryType.EXPENSE,
                        periodicity = RecurrenceType.MONTHLY,
                        paymentType = PaymentType.RECURRING,
                        qtyExecuted = 0,
                        qtyLimit = null,
                        lastExecution = null,
                        nextExecution = LocalDate.of(2026, 2, 1),
                        endExecution = null,
                        seriesId = UUID.randomUUID(),
                        seriesOffset = 0,
                    ),
                ).awaitSingle()
        }

        suspend fun personalRecurrence(userId: UUID) {
            dsl.recurrenceEventRepository
                .save(
                    RecurrenceEventEntity(
                        name = "Subscription",
                        categoryId = null,
                        createdByUserId = userId,
                        groupId = null,
                        tags = null,
                        observations = null,
                        type = WalletEntryType.EXPENSE,
                        periodicity = RecurrenceType.MONTHLY,
                        paymentType = PaymentType.RECURRING,
                        qtyExecuted = 0,
                        qtyLimit = null,
                        lastExecution = null,
                        nextExecution = LocalDate.of(2026, 2, 1),
                        endExecution = null,
                        seriesId = UUID.randomUUID(),
                        seriesOffset = 0,
                    ),
                ).awaitSingle()
        }
    }

    class When(
        private val dsl: AccountDeletionScenarioDsl,
    ) {
        suspend fun accountDeleted(userId: UUID) {
            dsl.accountDeletionService.deleteAccountForUser(userId)
        }
    }

    class Then(
        private val dsl: AccountDeletionScenarioDsl,
    ) {
        suspend fun userShouldNotExist(userId: UUID) {
            Assertions.assertFalse(dsl.userRepository.existsById(userId).awaitSingle())
        }

        suspend fun groupShouldNotExist(groupId: UUID) {
            Assertions.assertFalse(dsl.groupStore.existsById(groupId).awaitSingle())
        }

        suspend fun groupShouldExist(groupId: UUID) {
            Assertions.assertTrue(dsl.groupStore.existsById(groupId).awaitSingle())
        }

        suspend fun memberShouldHaveRole(
            groupId: UUID,
            userId: UUID,
            role: UserGroupRole,
        ) {
            val gu = dsl.groupStore.findOneByGroupIdAndUserId(groupId, userId).awaitSingleOrNull()
            Assertions.assertNotNull(gu)
            Assertions.assertEquals(role, gu!!.role)
        }

        suspend fun memberShouldNotExist(
            groupId: UUID,
            userId: UUID,
        ) {
            Assertions.assertNull(dsl.groupStore.findOneByGroupIdAndUserId(groupId, userId).awaitSingleOrNull())
        }

        suspend fun noWalletEventsForUserInGroup(
            userId: UUID,
            groupId: UUID,
        ) {
            val count =
                dsl.walletEventRepository.snapshot().count {
                    it.createdByUserId == userId && it.groupId == groupId
                }
            Assertions.assertEquals(0, count)
        }

        suspend fun noWalletEventsForUser(userId: UUID) {
            val count = dsl.walletEventRepository.snapshot().count { it.createdByUserId == userId }
            Assertions.assertEquals(0, count)
        }

        suspend fun noRecurrenceEventsForUser(userId: UUID) {
            val list =
                dsl.recurrenceEventRepository
                    .findAllEntries(
                        scope = WalletTransactionQueryScope.ownership(ownerUserIds = setOf(userId)),
                        minimumEndExecution = null,
                        maximumNextExecution = null,
                        billDate = null,
                        walletItemId = null,
                        walletItemIds = emptySet(),
                        entryTypes = emptySet(),
                    ).collectList()
                    .awaitSingle()
            Assertions.assertTrue(list.isEmpty())
        }

        suspend fun noRecurrenceEventsForUserInGroup(
            userId: UUID,
            groupId: UUID,
        ) {
            val list =
                dsl.recurrenceEventRepository
                    .findAllEntries(
                        scope = WalletTransactionQueryScope.group(groupIds = setOf(groupId)),
                        minimumEndExecution = null,
                        maximumNextExecution = null,
                        billDate = null,
                        walletItemId = null,
                        walletItemIds = emptySet(),
                        entryTypes = emptySet(),
                    ).collectList()
                    .awaitSingle()
            Assertions.assertTrue(list.none { event -> event.createdByUserId == userId && event.groupId == groupId })
        }

        fun complianceCleanupRecordedFor(userId: UUID) {
            Assertions.assertTrue(dsl.simulationJobService.complianceUserIds.contains(userId))
        }

        fun complianceCleanupNotRecordedFor(userId: UUID) {
            Assertions.assertFalse(dsl.simulationJobService.complianceUserIds.contains(userId))
        }

        fun avatarDeletionRecordedFor(userId: UUID) {
            Assertions.assertTrue(dsl.avatarService.deletedUserIds.contains(userId))
        }

        fun avatarDeletionNotRecordedFor(userId: UUID) {
            Assertions.assertFalse(dsl.avatarService.deletedUserIds.contains(userId))
        }
    }
}
