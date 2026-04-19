package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidDebtSettlementException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidWalletBeneficiarySplitException
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class WalletEntrySaveServiceImplTest {
    @Test
    fun `prepareMutationRequest should default group beneficiaries to author`() =
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val groupMateId = UUID.randomUUID()
            val bankAccount = bankAccount(id = UUID.randomUUID(), userId = actorUserId, currency = "BRL")
            val fixture =
                Fixture(
                    items = listOf(bankAccount),
                    members = listOf(actorUserId, groupMateId),
                )

            val prepared =
                fixture.service.prepare(
                    userId = actorUserId,
                    request =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = fixture.groupId,
                            originId = requireNotNull(bankAccount.id),
                            name = "Shared dinner",
                            date = LocalDate.of(2026, 4, 10),
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                        ),
                )

            assertThat(prepared.beneficiaries)
                .singleElement()
                .satisfies({ beneficiary ->
                    assertThat(beneficiary.userId).isEqualTo(actorUserId)
                    assertThat(beneficiary.benefitPercent).isEqualByComparingTo("100.00")
                })
            assertThat(prepared.resolvedBeneficiaries)
                .singleElement()
                .satisfies({ beneficiary ->
                    assertThat(beneficiary.userId).isEqualTo(actorUserId)
                    assertThat(beneficiary.benefitPercent).isEqualByComparingTo("100.00")
                })
        }

    @Test
    fun `prepareMutationRequest should reject beneficiary outside group`() {
        val actorUserId = UUID.randomUUID()
        val outsiderUserId = UUID.randomUUID()
        val bankAccount = bankAccount(id = UUID.randomUUID(), userId = actorUserId, currency = "BRL")
        val fixture =
            Fixture(
                items = listOf(bankAccount),
                members = listOf(actorUserId),
            )

        assertThatThrownBy {
            runBlocking {
                fixture.service.prepare(
                    userId = actorUserId,
                    request =
                        NewEntryRequest(
                            type = WalletEntryType.EXPENSE,
                            groupId = fixture.groupId,
                            originId = requireNotNull(bankAccount.id),
                            name = "Shared dinner",
                            date = LocalDate.of(2026, 4, 10),
                            value = BigDecimal("50.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                            beneficiaries =
                                listOf(
                                    NewWalletBeneficiaryLeg(
                                        userId = outsiderUserId,
                                        benefitPercent = BigDecimal("100.00"),
                                    ),
                                ),
                        ),
                )
            }
        }.isInstanceOf(InvalidWalletBeneficiarySplitException::class.java)
    }

    @Test
    fun `prepareMutationRequest should reject mixed-currency debt settlement`() {
        val actorUserId = UUID.randomUUID()
        val receiverUserId = UUID.randomUUID()
        val origin = bankAccount(id = UUID.randomUUID(), userId = actorUserId, currency = "BRL")
        val target = bankAccount(id = UUID.randomUUID(), userId = receiverUserId, currency = "USD")
        val fixture =
            Fixture(
                items = listOf(origin, target),
                members = listOf(actorUserId, receiverUserId),
            )

        assertThatThrownBy {
            runBlocking {
                fixture.service.prepare(
                    userId = actorUserId,
                    request =
                        NewEntryRequest(
                            type = WalletEntryType.TRANSFER,
                            groupId = fixture.groupId,
                            originId = requireNotNull(origin.id),
                            targetId = requireNotNull(target.id),
                            name = "Debt settlement",
                            date = LocalDate.of(2026, 4, 10),
                            originValue = BigDecimal("25.00"),
                            targetValue = BigDecimal("25.00"),
                            confirmed = true,
                            paymentType = PaymentType.UNIQUE,
                            transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
                        ),
                )
            }
        }.isInstanceOf(InvalidDebtSettlementException::class.java)
    }

    private class Fixture(
        items: List<WalletItem>,
        members: List<UUID>,
    ) {
        val groupId: UUID = UUID.randomUUID()
        private val group =
            GroupWithRole(
                id = groupId,
                createdAt = OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                updatedAt = OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                name = "Shared group",
                role = UserGroupRole.EDITOR,
                itemsAssociated = items,
            ).also { it.permissions = setOf(GroupPermissions.SEND_ENTRIES) }
        private val groupMembers =
            members.map { memberId ->
                GroupUserEntity(
                    groupId = groupId,
                    userId = memberId,
                    role = UserGroupRole.EDITOR,
                    allowPlanningSimulator = true,
                )
            }

        val service =
            TestWalletEntrySaveService(
                groupService = FakeGroupService(group, groupMembers),
                walletItemService = FakeWalletItemService(items.associateBy { requireNotNull(it.id) }),
                creditCardBillService = mock(CreditCardBillService::class.java),
                recurrenceService = mock(RecurrenceService::class.java),
                recurrenceEventRepository = mock(RecurrenceEventRepository::class.java),
                recurrenceSeriesRepository = mock(RecurrenceSeriesRepository::class.java),
                recurrenceEntryRepository = mock(RecurrenceEntryRepository::class.java),
                walletEventBeneficiaryRepository = mock(WalletEventBeneficiarySpringDataRepository::class.java),
                recurrenceEventBeneficiaryRepository = mock(RecurrenceEventBeneficiarySpringDataRepository::class.java),
            )
    }

    private class TestWalletEntrySaveService(
        groupService: GroupService,
        walletItemService: WalletItemService,
        creditCardBillService: CreditCardBillService,
        recurrenceService: RecurrenceService,
        recurrenceEventRepository: RecurrenceEventRepository,
        recurrenceSeriesRepository: RecurrenceSeriesRepository,
        recurrenceEntryRepository: RecurrenceEntryRepository,
        walletEventBeneficiaryRepository: WalletEventBeneficiarySpringDataRepository,
        recurrenceEventBeneficiaryRepository: RecurrenceEventBeneficiarySpringDataRepository,
    ) : WalletEntrySaveServiceImpl(
            groupService = groupService,
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            recurrenceService = recurrenceService,
            recurrenceEventRepository = recurrenceEventRepository,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
            recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
            clock = Clock.fixed(Instant.parse("2026-04-10T12:00:00Z"), ZoneOffset.UTC),
        ) {
        suspend fun prepare(
            userId: UUID,
            request: NewEntryRequest,
        ): NewEntryRequest = prepareMutationRequest(userId, request)
    }

    private class FakeWalletItemService(
        private val itemsById: Map<UUID, WalletItem>,
    ) : WalletItemService {
        override suspend fun findAllItems(
            userId: UUID,
            pageable: Pageable,
            onlyBankAccounts: Boolean,
        ): Page<WalletItem> = Page.empty(pageable)

        override suspend fun findOne(id: UUID): WalletItem? = itemsById[id]

        override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletItem> = emptyFlow()

        override suspend fun addBalanceById(
            id: UUID,
            balance: BigDecimal,
        ): Long = 0L
    }

    private class FakeGroupService(
        private val group: GroupWithRole,
        private val members: List<GroupUserEntity>,
    ) : GroupService {
        override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> = emptyList()

        override suspend fun searchGroups(
            userId: UUID,
            pageable: Pageable,
            query: String?,
        ): Page<GroupWithRole> = Page.empty(pageable)

        override suspend fun findGroup(
            userId: UUID,
            id: UUID,
        ): GroupWithRole? = null

        override suspend fun findGroupWithAssociatedItems(
            userId: UUID,
            id: UUID,
        ): GroupWithRole? = if (id == group.id) group else null

        override suspend fun editGroup(
            userId: UUID,
            id: UUID,
            request: EditGroupRequest,
        ): GroupWithRole? = null

        override suspend fun deleteGroup(
            userId: UUID,
            id: UUID,
        ): Boolean = false

        override suspend fun newGroup(
            userId: UUID,
            newGroupRequest: NewGroupRequest,
        ): GroupEntity = error("Not used in tests")

        override suspend fun findAllMembers(
            userId: UUID,
            id: UUID,
        ): List<GroupUserEntity> = if (id == group.id) members else emptyList()

        override suspend fun updateMemberRole(
            userId: UUID,
            id: UUID,
            memberId: UUID,
            newRole: UserGroupRole,
        ): Boolean = false

        override suspend fun addNewMember(
            userId: UUID,
            id: UUID,
            role: UserGroupRole,
        ) = Unit

        override suspend fun updateOwnPlanningSimulatorOptIn(
            userId: UUID,
            id: UUID,
            allowPlanningSimulator: Boolean,
        ): Boolean = false

        override fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity> = emptyFlow()
    }

    private fun bankAccount(
        id: UUID,
        userId: UUID,
        currency: String,
    ): WalletItem =
        BankAccount(
            name = "Wallet $currency",
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal("1000.00"),
        ).also { it.id = id }
}
