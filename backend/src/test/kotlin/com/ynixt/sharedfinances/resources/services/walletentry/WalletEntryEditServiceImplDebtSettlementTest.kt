package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptKind
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventBeneficiarySpringDataRepository
import com.ynixt.sharedfinances.resources.services.groups.GroupDebtLedgerMaintenanceService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

class WalletEntryEditServiceImplDebtSettlementTest {
    @Test
    fun `editing a debt settlement should replace prior debt fragments instead of preserving them`() {
        runBlocking {
            val actorUserId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val originWalletId = UUID.randomUUID()
            val targetWalletId = UUID.randomUUID()
            val debtCategoryConceptId = UUID.randomUUID()
            val debtCategoryId = UUID.randomUUID()

            val originWallet =
                BankAccount(
                    name = "Origin",
                    enabled = true,
                    userId = actorUserId,
                    currency = "BRL",
                    balance = BigDecimal("100.00"),
                ).also { it.id = originWalletId }
            val targetWallet =
                BankAccount(
                    name = "Target",
                    enabled = true,
                    userId = UUID.randomUUID(),
                    currency = "BRL",
                    balance = BigDecimal("0.00"),
                ).also { it.id = targetWalletId }

            val originWalletEntity =
                WalletItemEntity(
                    type = WalletItemType.BANK_ACCOUNT,
                    name = "Origin",
                    enabled = true,
                    userId = originWallet.userId,
                    currency = "BRL",
                    balance = BigDecimal("100.00"),
                    totalLimit = null,
                    dueDay = null,
                    daysBetweenDueAndClosing = null,
                    dueOnNextBusinessDay = null,
                ).also { it.id = originWalletId }
            val targetWalletEntity =
                WalletItemEntity(
                    type = WalletItemType.BANK_ACCOUNT,
                    name = "Target",
                    enabled = true,
                    userId = targetWallet.userId,
                    currency = "BRL",
                    balance = BigDecimal("0.00"),
                    totalLimit = null,
                    dueDay = null,
                    daysBetweenDueAndClosing = null,
                    dueOnNextBusinessDay = null,
                ).also { it.id = targetWalletId }

            val debtCategoryConcept =
                WalletCategoryConceptEntity(
                    kind = WalletCategoryConceptKind.PREDEFINED,
                    code = WalletCategoryConceptCode.DEBT_SF,
                    displayName = "Debt",
                ).also { it.id = debtCategoryConceptId }
            val debtCategory =
                WalletEntryCategoryEntity(
                    name = "Debt",
                    color = "#000000",
                    userId = null,
                    groupId = groupId,
                    parentId = null,
                    conceptId = debtCategoryConceptId,
                ).also { it.id = debtCategoryId }

            val existingEvent =
                WalletEventEntity(
                    type = WalletEntryType.TRANSFER,
                    name = "Debt settlement",
                    categoryId = debtCategoryId,
                    createdByUserId = actorUserId,
                    groupId = groupId,
                    tags = null,
                    observations = null,
                    date = LocalDate.of(2026, 7, 10),
                    confirmed = true,
                    installment = null,
                    recurrenceEventId = null,
                    paymentType = PaymentType.UNIQUE,
                    transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
                ).also {
                    it.id = eventId
                    it.createdAt = OffsetDateTime.of(2026, 7, 10, 10, 0, 0, 0, ZoneOffset.UTC)
                }

            val oldOriginEntry =
                WalletEntryEntity(
                    value = BigDecimal("-0.03"),
                    walletEventId = eventId,
                    walletItemId = originWalletId,
                    billId = null,
                )
            val oldTargetEntry =
                WalletEntryEntity(
                    value = BigDecimal("0.03"),
                    walletEventId = eventId,
                    walletItemId = targetWalletId,
                    billId = null,
                )

            val oldSettlementMovement =
                GroupMemberDebtMovementEntity(
                    groupId = groupId,
                    payerId = actorUserId,
                    receiverId = targetWallet.userId,
                    month = LocalDate.of(2026, 4, 1),
                    currency = "BRL",
                    deltaSigned = BigDecimal("-0.03"),
                    reasonKind = GroupDebtMovementReasonKind.DEBT_SETTLEMENT,
                    createdByUserId = actorUserId,
                    sourceWalletEventId = eventId,
                ).also { it.id = UUID.randomUUID() }

            val walletEventRepository = Mockito.mock(WalletEventRepository::class.java)
            val walletEntryRepository = Mockito.mock(WalletEntryRepository::class.java)
            val walletEventBeneficiaryRepository = Mockito.mock(WalletEventBeneficiarySpringDataRepository::class.java)
            val recurrenceEventBeneficiaryRepository = Mockito.mock(RecurrenceEventBeneficiarySpringDataRepository::class.java)
            val walletEventActionEventService = Mockito.mock(WalletEventActionEventService::class.java)
            val walletEntryCreateService = Mockito.mock(WalletEntryCreateService::class.java)
            val walletItemMapper = Mockito.mock(WalletItemMapper::class.java)
            val groupDebtService = Mockito.mock(GroupDebtService::class.java)
            val groupService = Mockito.mock(GroupService::class.java)
            val walletItemService = Mockito.mock(WalletItemService::class.java)
            val genericCategoryService = Mockito.mock(GenericCategoryService::class.java)
            val categoryConceptService = Mockito.mock(CategoryConceptService::class.java)
            val creditCardBillService = Mockito.mock(CreditCardBillService::class.java)
            val recurrenceService = Mockito.mock(RecurrenceService::class.java)
            val recurrenceEventRepository = Mockito.mock(RecurrenceEventRepository::class.java)
            val recurrenceSeriesRepository = Mockito.mock(RecurrenceSeriesRepository::class.java)
            val recurrenceEntryRepository = Mockito.mock(RecurrenceEntryRepository::class.java)
            val debtMovementRepository = Mockito.mock(GroupMemberDebtMovementSpringDataRepository::class.java)
            val debtDatabaseClientRepository = Mockito.mock(GroupMemberDebtDatabaseClientRepository::class.java)

            Mockito
                .`when`(walletEntryRepository.findAllByWalletEventId(eventId))
                .thenReturn(Flux.just(oldOriginEntry, oldTargetEntry))
            Mockito
                .`when`(walletEventBeneficiaryRepository.findAllByWalletEventId(eventId))
                .thenReturn(Flux.empty())
            Mockito
                .`when`(walletEventRepository.save(anyNonNull()))
                .thenAnswer { invocation -> Mono.just(invocation.getArgument(0)) }
            Mockito
                .`when`(walletEntryRepository.deleteAllByWalletEventId(eventId))
                .thenReturn(Mono.just(2))
            Mockito
                .`when`(walletEventBeneficiaryRepository.deleteAllByWalletEventId(eventId))
                .thenReturn(Mono.just(0))
            Mockito
                .`when`(walletEntryRepository.saveAll(anyNonNull<Iterable<WalletEntryEntity>>()))
                .thenAnswer { invocation ->
                    val entries =
                        (invocation.getArgument<Iterable<WalletEntryEntity>>(0))
                            .mapIndexed { index, entry ->
                                entry.also { persisted ->
                                    persisted.id = UUID.nameUUIDFromBytes("new-entry-$index".toByteArray())
                                }
                            }
                    Flux.fromIterable(entries)
                }
            Mockito
                .`when`(walletEventBeneficiaryRepository.saveAll(anyNonNull<Iterable<WalletEventBeneficiaryEntity>>()))
                .thenAnswer { invocation ->
                    Flux.fromIterable(invocation.getArgument<Iterable<WalletEventBeneficiaryEntity>>(0))
                }
            Mockito
                .`when`(walletItemService.findAllByIdIn(setOf(originWalletId, targetWalletId)))
                .thenReturn(kotlinx.coroutines.flow.flowOf(originWallet, targetWallet))
            Mockito
                .`when`(walletItemService.addBalanceById(anyNonNull(), anyNonNull()))
                .thenReturn(1L)
            Mockito
                .`when`(debtMovementRepository.findAllBySourceWalletEventId(eventId))
                .thenReturn(Flux.just(oldSettlementMovement))
            Mockito
                .`when`(debtMovementRepository.deleteById(requireNotNull(oldSettlementMovement.id).toString()))
                .thenReturn(Mono.empty())
            Mockito
                .`when`(
                    debtDatabaseClientRepository.sumMovementBalanceForScope(
                        groupId,
                        actorUserId,
                        targetWallet.userId,
                        LocalDate.of(2026, 4, 1),
                        "BRL",
                    ),
                ).thenReturn(Mono.just(BigDecimal.ZERO))
            Mockito
                .`when`(
                    debtDatabaseClientRepository.deleteMonthlyBalance(
                        groupId,
                        actorUserId,
                        targetWallet.userId,
                        LocalDate.of(2026, 4, 1),
                        "BRL",
                    ),
                ).thenReturn(Mono.just(1L))
            Mockito.`when`(walletItemMapper.fromModel(originWallet)).thenReturn(originWalletEntity)
            Mockito.`when`(walletItemMapper.fromModel(targetWallet)).thenReturn(targetWalletEntity)

            val service =
                WalletEntryEditServiceImpl(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletEventActionEventService = walletEventActionEventService,
                    walletEntryCreateService = walletEntryCreateService,
                    walletItemMapper = walletItemMapper,
                    groupDebtService = groupDebtService,
                    groupService = groupService,
                    walletItemService = walletItemService,
                    genericCategoryService = genericCategoryService,
                    categoryConceptService = categoryConceptService,
                    creditCardBillService = creditCardBillService,
                    recurrenceService = recurrenceService,
                    recurrenceEventRepository = recurrenceEventRepository,
                    recurrenceSeriesRepository = recurrenceSeriesRepository,
                    recurrenceEntryRepository = recurrenceEntryRepository,
                    walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
                    recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
                    debtMovementRepository = debtMovementRepository,
                    debtLedgerMaintenanceService =
                        GroupDebtLedgerMaintenanceService(
                            debtMovementRepository,
                            debtDatabaseClientRepository,
                        ),
                    clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC),
                )

            val group =
                GroupWithRole(
                    id = groupId,
                    createdAt = null,
                    updatedAt = null,
                    name = "Group",
                    role = UserGroupRole.ADMIN,
                    itemsAssociated = listOf(originWallet, targetWallet),
                ).also {
                    it.permissions = setOf(GroupPermissions.SEND_ENTRIES)
                }

            val preparedRequest =
                NewEntryRequest(
                    type = WalletEntryType.TRANSFER,
                    groupId = groupId,
                    originId = originWalletId,
                    targetId = targetWalletId,
                    name = "Debt settlement",
                    categoryId = debtCategoryId,
                    date = LocalDate.of(2026, 7, 11),
                    originValue = BigDecimal("2.03"),
                    confirmed = true,
                    paymentType = PaymentType.UNIQUE,
                    transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
                    group = group,
                    origin = originWallet,
                    target = targetWallet,
                    category = debtCategory,
                )

            val editPostedEvent =
                WalletEntryEditServiceImpl::class
                    .declaredFunctions
                    .first { function -> function.name == "editPostedEvent" }
                    .also { function -> function.isAccessible = true }

            val updated =
                editPostedEvent.callSuspend(
                    service,
                    actorUserId,
                    existingEvent,
                    preparedRequest,
                    null,
                    null,
                ) as WalletEventEntity

            assertThat(updated.id).isEqualTo(eventId)
            Mockito.verify(groupDebtService, Mockito.never()).rollbackWalletEvent(anyNonNull(), anyNonNull())
            Mockito.verify(debtMovementRepository).findAllBySourceWalletEventId(eventId)
            Mockito.verify(debtMovementRepository).deleteById(requireNotNull(oldSettlementMovement.id).toString())
            Mockito.verify(groupDebtService).applyWalletEvent(actorUserId, updated, updated.entries!!.filterIsInstance<WalletEntryEntity>())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = Mockito.any<T>() ?: null as T
}
