package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.walletentry.SimulatedOccurrence
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceOccurrenceSimulationService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class RecurrenceSimulationServiceImplTest {
    @Test
    fun `simulateGeneration should reject userIds when groupIds is empty`() =
        runBlocking {
            val recurrenceService = mock(RecurrenceService::class.java)
            val service = createService(recurrenceService = recurrenceService)

            org.assertj.core.api.Assertions
                .assertThatThrownBy {
                    runBlocking {
                        service.simulateGeneration(
                            minimumEndExecution = LocalDate.of(2026, 4, 1),
                            maximumNextExecution = LocalDate.of(2026, 4, 30),
                            userId = UUID.randomUUID(),
                            groupIds = emptySet(),
                            userIds = setOf(UUID.randomUUID()),
                            walletItemId = null,
                            billDate = null,
                        )
                    }
                }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Filter userIds requires at least one groupId")

            verify(recurrenceService, never()).findAllEntries(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anySet(),
                org.mockito.ArgumentMatchers.anySet(),
                org.mockito.ArgumentMatchers.anySet(),
                org.mockito.ArgumentMatchers.anySet(),
                org.mockito.ArgumentMatchers.any(),
            )
        }

    @Test
    fun `simulateGeneration should perform a single set-based recurrence query for group plus userIds`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val ownerA = UUID.randomUUID()
            val ownerB = UUID.randomUUID()
            val groupA = UUID.randomUUID()
            val groupB = UUID.randomUUID()
            val minimum = LocalDate.of(2026, 4, 1)
            val maximum = LocalDate.of(2026, 6, 1)
            val fixedStart = LocalDate.of(2026, 4, 2)
            val expectedSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

            val recurrenceService = mock(RecurrenceService::class.java)
            `when`(
                recurrenceService.findAllEntries(
                    fixedStart,
                    maximum,
                    null,
                    null,
                    emptySet(),
                    setOf(ownerA, ownerB),
                    setOf(groupA, groupB),
                    emptySet(),
                    expectedSort,
                ),
            ).thenReturn(emptyFlow())

            val service = createService(recurrenceService = recurrenceService)

            val result =
                service.simulateGeneration(
                    minimumEndExecution = minimum,
                    maximumNextExecution = maximum,
                    userId = requesterId,
                    groupIds = setOf(groupA, groupB),
                    userIds = setOf(ownerA, ownerB),
                    walletItemId = null,
                    billDate = null,
                )

            assertThat(result).isEmpty()
            verify(recurrenceService, times(1)).findAllEntries(
                fixedStart,
                maximum,
                null,
                null,
                emptySet(),
                setOf(ownerA, ownerB),
                setOf(groupA, groupB),
                emptySet(),
                expectedSort,
            )
        }

    @Test
    fun `simulateGeneration should batch load series totals and avoid per-config findById`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val walletId = UUID.randomUUID()
            val nowClock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC)
            val minimum = LocalDate.of(2026, 4, 1)
            val maximum = LocalDate.of(2026, 6, 1)
            val fixedStart = LocalDate.of(2026, 4, 2)
            val expectedSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

            val configA = recurrenceConfig(seriesId = UUID.randomUUID(), walletItemId = walletId, nextExecution = LocalDate.of(2026, 4, 10))
            val configB = recurrenceConfig(seriesId = UUID.randomUUID(), walletItemId = walletId, nextExecution = LocalDate.of(2026, 4, 11))
            val configC = recurrenceConfig(seriesId = UUID.randomUUID(), walletItemId = walletId, nextExecution = LocalDate.of(2026, 4, 12))
            val configs = listOf(configA, configB, configC)
            val expectedSeriesIds = configs.map { it.seriesId }.toSet()

            val recurrenceService = mock(RecurrenceService::class.java)
            `when`(
                recurrenceService.findAllEntries(
                    fixedStart,
                    maximum,
                    null,
                    null,
                    emptySet(),
                    setOf(userId),
                    emptySet(),
                    emptySet(),
                    expectedSort,
                ),
            ).thenReturn(flowOf(configA, configB, configC))

            val recurrenceSeriesRepository = mock(RecurrenceSeriesRepository::class.java)
            `when`(
                recurrenceSeriesRepository.findAllByIdIn(expectedSeriesIds),
            ).thenReturn(
                Flux.fromIterable(
                    configs.mapIndexed { index, cfg ->
                        RecurrenceSeriesEntity(qtyTotal = 12 + index).also { it.id = cfg.seriesId }
                    },
                ),
            )

            val walletItemService = mock(WalletItemService::class.java)
            `when`(walletItemService.findAllByIdIn(setOf(walletId))).thenReturn(
                flowOf(
                    BankAccount(
                        name = "Main",
                        enabled = true,
                        userId = userId,
                        currency = "BRL",
                        balance = BigDecimal.ZERO,
                    ).also { it.id = walletId },
                ),
            )

            val recurrenceOccurrenceSimulationService = mock(RecurrenceOccurrenceSimulationService::class.java)
            `when`(
                recurrenceOccurrenceSimulationService.buildOccurrences(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyList(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                ),
            ).thenAnswer { invocation ->
                val config = invocation.getArgument<RecurrenceEventEntity>(0)
                listOf(
                    SimulatedOccurrence(
                        executionDate = config.nextExecution!!,
                        installment = null,
                        billDateByWalletItemId = mapOf(walletId to null),
                    ),
                )
            }

            val service =
                RecurrenceSimulationServiceImpl(
                    genericCategoryService =
                        mock(GenericCategoryService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    groupService =
                        mock(GroupService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    userService =
                        mock(UserService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    walletItemService = walletItemService,
                    creditCardBillService = mock(CreditCardBillService::class.java),
                    walletItemMapper = mock(WalletItemMapper::class.java),
                    recurrenceService = recurrenceService,
                    recurrenceOccurrenceSimulationService = recurrenceOccurrenceSimulationService,
                    recurrenceSeriesRepository = recurrenceSeriesRepository,
                    clock = nowClock,
                )

            val responses =
                service.simulateGeneration(
                    minimumEndExecution = minimum,
                    maximumNextExecution = maximum,
                    userId = userId,
                    groupIds = emptySet(),
                    userIds = emptySet(),
                    walletItemId = null,
                    billDate = null,
                )

            assertThat(responses).hasSize(3)
            verify(recurrenceSeriesRepository, times(1)).findAllByIdIn(expectedSeriesIds)
            verify(recurrenceSeriesRepository, never()).findById(org.mockito.ArgumentMatchers.any())
        }

    @Test
    fun `simulateGeneration should reuse wallet items hydrated from recurrence query`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val walletId = UUID.randomUUID()
            val nowClock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC)
            val minimum = LocalDate.of(2026, 4, 1)
            val maximum = LocalDate.of(2026, 6, 1)
            val fixedStart = LocalDate.of(2026, 4, 2)
            val expectedSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

            val config =
                recurrenceConfig(
                    seriesId = UUID.randomUUID(),
                    walletItemId = walletId,
                    nextExecution = LocalDate.of(2026, 4, 10),
                )
            val hydratedWalletItemEntity =
                WalletItemEntity(
                    type = WalletItemType.BANK_ACCOUNT,
                    name = "Main",
                    enabled = true,
                    userId = userId,
                    currency = "BRL",
                    balance = BigDecimal.ZERO,
                    totalLimit = null,
                    dueDay = null,
                    daysBetweenDueAndClosing = null,
                    dueOnNextBusinessDay = null,
                    showOnDashboard = true,
                ).also { it.id = walletId }
            config.entries!!.first().walletItem = hydratedWalletItemEntity

            val recurrenceService = mock(RecurrenceService::class.java)
            `when`(
                recurrenceService.findAllEntries(
                    fixedStart,
                    maximum,
                    null,
                    null,
                    emptySet(),
                    setOf(userId),
                    emptySet(),
                    emptySet(),
                    expectedSort,
                ),
            ).thenReturn(flowOf(config))

            val recurrenceSeriesRepository = mock(RecurrenceSeriesRepository::class.java)
            `when`(recurrenceSeriesRepository.findAllByIdIn(setOf(config.seriesId))).thenReturn(
                Flux.just(
                    RecurrenceSeriesEntity(qtyTotal = 12).also { it.id = config.seriesId },
                ),
            )

            val walletItemService = mock(WalletItemService::class.java)
            val walletItemMapper = mock(WalletItemMapper::class.java)
            `when`(walletItemMapper.toModel(hydratedWalletItemEntity)).thenReturn(
                BankAccount(
                    name = "Main",
                    enabled = true,
                    userId = userId,
                    currency = "BRL",
                    balance = BigDecimal.ZERO,
                ).also { it.id = walletId },
            )

            val recurrenceOccurrenceSimulationService = mock(RecurrenceOccurrenceSimulationService::class.java)
            `when`(
                recurrenceOccurrenceSimulationService.buildOccurrences(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyList(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                ),
            ).thenReturn(
                listOf(
                    SimulatedOccurrence(
                        executionDate = config.nextExecution!!,
                        installment = null,
                        billDateByWalletItemId = mapOf(walletId to null),
                    ),
                ),
            )

            val service =
                RecurrenceSimulationServiceImpl(
                    genericCategoryService =
                        mock(GenericCategoryService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    groupService =
                        mock(GroupService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    userService =
                        mock(UserService::class.java).also {
                            `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                        },
                    walletItemService = walletItemService,
                    creditCardBillService = mock(CreditCardBillService::class.java),
                    walletItemMapper = walletItemMapper,
                    recurrenceService = recurrenceService,
                    recurrenceOccurrenceSimulationService = recurrenceOccurrenceSimulationService,
                    recurrenceSeriesRepository = recurrenceSeriesRepository,
                    clock = nowClock,
                )

            val responses =
                service.simulateGeneration(
                    minimumEndExecution = minimum,
                    maximumNextExecution = maximum,
                    userId = userId,
                    groupIds = emptySet(),
                    userIds = emptySet(),
                    walletItemId = null,
                    billDate = null,
                )

            assertThat(responses).hasSize(1)
            verify(walletItemMapper, times(1)).toModel(hydratedWalletItemEntity)
            verify(walletItemService, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anyCollection())
        }

    @Test
    fun `simulateGeneration should reuse hydrated series and metadata from recurrence query`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val walletId = UUID.randomUUID()
            val nowClock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC)
            val minimum = LocalDate.of(2026, 4, 1)
            val maximum = LocalDate.of(2026, 6, 1)
            val fixedStart = LocalDate.of(2026, 4, 2)
            val expectedSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

            val config =
                recurrenceConfig(
                    seriesId = UUID.randomUUID(),
                    walletItemId = walletId,
                    nextExecution = LocalDate.of(2026, 4, 10),
                )
            config.seriesQtyTotal = 12

            val hydratedWalletItemEntity =
                WalletItemEntity(
                    type = WalletItemType.BANK_ACCOUNT,
                    name = "Main",
                    enabled = true,
                    userId = userId,
                    currency = "BRL",
                    balance = BigDecimal.ZERO,
                    totalLimit = null,
                    dueDay = null,
                    daysBetweenDueAndClosing = null,
                    dueOnNextBusinessDay = null,
                    showOnDashboard = true,
                ).also { it.id = walletId }
            config.entries!!.first().walletItem = hydratedWalletItemEntity

            val hydratedCategory =
                WalletEntryCategoryEntity(
                    name = "Food",
                    color = "#334155",
                    userId = userId,
                    groupId = null,
                    parentId = null,
                ).also { it.id = UUID.randomUUID() }
            val hydratedGroup =
                GroupEntity(
                    name = "Home",
                ).also { it.id = UUID.randomUUID() }
            val hydratedUser =
                UserEntity(
                    email = "hydrated@test.local",
                    passwordHash = "hash",
                    firstName = "Hydrated",
                    lastName = "User",
                    lang = "en",
                    defaultCurrency = "BRL",
                    tmz = "UTC",
                    photoUrl = null,
                    emailVerified = true,
                    mfaEnabled = false,
                    totpSecret = null,
                    onboardingDone = true,
                ).also { it.id = config.createdByUserId }

            config.hydratedCategory = hydratedCategory
            config.hydratedGroup = hydratedGroup
            config.hydratedUser = hydratedUser

            val recurrenceService = mock(RecurrenceService::class.java)
            `when`(
                recurrenceService.findAllEntries(
                    fixedStart,
                    maximum,
                    null,
                    null,
                    emptySet(),
                    setOf(userId),
                    emptySet(),
                    emptySet(),
                    expectedSort,
                ),
            ).thenReturn(flowOf(config))

            val recurrenceSeriesRepository = mock(RecurrenceSeriesRepository::class.java)
            val genericCategoryService = mock(GenericCategoryService::class.java)
            val groupService = mock(GroupService::class.java)
            val userService = mock(UserService::class.java)
            val walletItemService = mock(WalletItemService::class.java)
            val walletItemMapper = mock(WalletItemMapper::class.java)
            `when`(walletItemMapper.toModel(hydratedWalletItemEntity)).thenReturn(
                BankAccount(
                    name = "Main",
                    enabled = true,
                    userId = userId,
                    currency = "BRL",
                    balance = BigDecimal.ZERO,
                ).also { it.id = walletId },
            )

            val recurrenceOccurrenceSimulationService = mock(RecurrenceOccurrenceSimulationService::class.java)
            `when`(
                recurrenceOccurrenceSimulationService.buildOccurrences(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyList(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                ),
            ).thenReturn(
                listOf(
                    SimulatedOccurrence(
                        executionDate = config.nextExecution!!,
                        installment = null,
                        billDateByWalletItemId = mapOf(walletId to null),
                    ),
                ),
            )

            val service =
                RecurrenceSimulationServiceImpl(
                    genericCategoryService = genericCategoryService,
                    groupService = groupService,
                    userService = userService,
                    walletItemService = walletItemService,
                    creditCardBillService = mock(CreditCardBillService::class.java),
                    walletItemMapper = walletItemMapper,
                    recurrenceService = recurrenceService,
                    recurrenceOccurrenceSimulationService = recurrenceOccurrenceSimulationService,
                    recurrenceSeriesRepository = recurrenceSeriesRepository,
                    clock = nowClock,
                )

            val responses =
                service.simulateGeneration(
                    minimumEndExecution = minimum,
                    maximumNextExecution = maximum,
                    userId = userId,
                    groupIds = emptySet(),
                    userIds = emptySet(),
                    walletItemId = null,
                    billDate = null,
                )

            assertThat(responses).hasSize(1)
            assertThat(responses.single().category).isSameAs(hydratedCategory)
            assertThat(responses.single().group).isSameAs(hydratedGroup)
            assertThat(responses.single().user).isSameAs(hydratedUser)
            verify(recurrenceSeriesRepository, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anySet())
            verify(genericCategoryService, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anySet())
            verify(groupService, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anySet())
            verify(userService, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anySet())
            verify(walletItemService, never()).findAllByIdIn(org.mockito.ArgumentMatchers.anyCollection())
        }

    private fun recurrenceConfig(
        seriesId: UUID,
        walletItemId: UUID,
        nextExecution: LocalDate,
    ): RecurrenceEventEntity {
        val event =
            RecurrenceEventEntity(
                name = "Recurring",
                categoryId = null,
                createdByUserId = UUID.randomUUID(),
                groupId = null,
                tags = emptyList(),
                observations = null,
                type = WalletEntryType.EXPENSE,
                periodicity = RecurrenceType.MONTHLY,
                paymentType = PaymentType.UNIQUE,
                qtyExecuted = 0,
                qtyLimit = null,
                lastExecution = null,
                nextExecution = nextExecution,
                endExecution = null,
                seriesId = seriesId,
                seriesOffset = 0,
            )
        event.id = UUID.randomUUID()
        event.entries =
            mutableListOf(
                RecurrenceEntryEntity(
                    value = BigDecimal("-10.00"),
                    walletEventId = event.id!!,
                    walletItemId = walletItemId,
                    nextBillDate = null,
                    lastBillDate = null,
                    contributionPercent = null,
                ).also { it.event = event },
            )
        return event
    }

    private fun createService(recurrenceService: RecurrenceService): RecurrenceSimulationServiceImpl =
        RecurrenceSimulationServiceImpl(
            genericCategoryService =
                mock(GenericCategoryService::class.java).also {
                    `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                },
            groupService =
                mock(GroupService::class.java).also {
                    `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                },
            userService =
                mock(UserService::class.java).also {
                    `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                },
            walletItemService =
                mock(WalletItemService::class.java).also {
                    `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                },
            creditCardBillService = mock(CreditCardBillService::class.java),
            walletItemMapper = mock(WalletItemMapper::class.java),
            recurrenceService = recurrenceService,
            recurrenceOccurrenceSimulationService = mock(RecurrenceOccurrenceSimulationService::class.java),
            recurrenceSeriesRepository = mock(RecurrenceSeriesRepository::class.java),
            clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneOffset.UTC),
        )
}
