package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceSeriesEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
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
                recurrenceService.findAllEntryByUserId(
                    fixedStart,
                    maximum,
                    userId,
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
                    genericCategoryService = mock(GenericCategoryService::class.java).also {
                        `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                    },
                    groupService = mock(GroupService::class.java).also {
                        `when`(it.findAllByIdIn(emptySet())).thenReturn(emptyFlow())
                    },
                    userService = mock(UserService::class.java).also {
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
                    groupId = null,
                    walletItemId = null,
                    billDate = null,
                )

            assertThat(responses).hasSize(3)
            verify(recurrenceSeriesRepository, times(1)).findAllByIdIn(expectedSeriesIds)
            verify(recurrenceSeriesRepository, never()).findById(org.mockito.ArgumentMatchers.any())
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
                userId = null,
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
}
