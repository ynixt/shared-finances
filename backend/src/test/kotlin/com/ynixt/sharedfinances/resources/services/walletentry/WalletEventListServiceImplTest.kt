package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.impl.BankAccountMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.CreditCardMapperImpl
import com.ynixt.sharedfinances.domain.mapper.impl.WalletItemMapperImpl
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.ListEntryRequest
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryCreditCardBillRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceSeriesRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.codec.multipart.FilePart
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class WalletEventListServiceImplTest {
    @Test
    fun `should compose optional filters on ownership path`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val bankId = UUID.randomUUID()
            val cardId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = emptySet(),
                    simulatedEvents = emptyList(),
                )

            val bankItem = bankAccountEntity(id = bankId, userId = requesterId, name = "Owned bank")
            val cardItem = creditCardEntity(id = cardId, userId = requesterId, name = "Owned card")
            listOf(bankItem, cardItem).forEach { walletItemRepository.save(it).block() }

            val matchId = UUID.fromString("00000000-0000-0000-0000-0000000000e1")
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = matchId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 10),
                        entries = listOf(walletEntry(walletEventId = matchId, walletItem = cardItem, value = "-50.00")),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = UUID.fromString("00000000-0000-0000-0000-0000000000e2"),
                        type = WalletEntryType.REVENUE,
                        createdByUserId = requesterId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 10),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = UUID.fromString("00000000-0000-0000-0000-0000000000e2"),
                                    walletItem = cardItem,
                                    value = "30.00",
                                ),
                            ),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = UUID.fromString("00000000-0000-0000-0000-0000000000e3"),
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = null,
                        date = LocalDate.of(2026, 3, 28),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = UUID.fromString("00000000-0000-0000-0000-0000000000e3"),
                                    walletItem = bankItem,
                                    value = "-20.00",
                                ),
                            ),
                    ),
            )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = cardId,
                            creditCardIds = setOf(cardId),
                            bankAccountIds = emptySet(),
                            entryTypes = setOf(WalletEntryType.EXPENSE),
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items.mapNotNull { it.id }).containsExactly(matchId)
        }

    @Test
    fun `should deduplicate multi-origin event on ownership path`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val bankAId = UUID.randomUUID()
            val bankBId = UUID.randomUUID()
            val eventId = UUID.fromString("00000000-0000-0000-0000-0000000000f1")

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = emptySet(),
                    simulatedEvents = emptyList(),
                )

            val bankA = bankAccountEntity(id = bankAId, userId = requesterId, name = "A")
            val bankB = bankAccountEntity(id = bankBId, userId = requesterId, name = "B")
            listOf(bankA, bankB).forEach { walletItemRepository.save(it).block() }

            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 14),
                        entries =
                            listOf(
                                walletEntry(walletEventId = eventId, walletItem = bankA, value = "-30.00"),
                                walletEntry(walletEventId = eventId, walletItem = bankB, value = "-20.00"),
                            ),
                    ),
            )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items.mapNotNull { it.id }).containsExactly(eventId)
        }

    @Test
    fun `should return owned group transaction without requiring group filter`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val bankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = emptySet(),
                    simulatedEvents = emptyList(),
                )

            val ownedBank = bankAccountEntity(id = bankId, userId = requesterId, name = "Owned bank")
            walletItemRepository.save(ownedBank).block()

            val eventId = UUID.fromString("00000000-0000-0000-0000-0000000000b1")
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = UUID.randomUUID(),
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 10),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = eventId,
                                    walletItem = ownedBank,
                                    value = "-45.00",
                                ),
                            ),
                    ),
            )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items.mapNotNull { it.id }).containsExactly(eventId)
        }

    @Test
    fun `should restrict group listing by userIds ownership filter`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val ownerA = UUID.randomUUID()
            val ownerB = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val ownerABankId = UUID.randomUUID()
            val ownerBBankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = setOf(groupId),
                    simulatedEvents = emptyList(),
                )

            val ownerAItem = bankAccountEntity(id = ownerABankId, userId = ownerA, name = "Owner A bank")
            val ownerBItem = bankAccountEntity(id = ownerBBankId, userId = ownerB, name = "Owner B bank")
            listOf(ownerAItem, ownerBItem).forEach { walletItemRepository.save(it).block() }

            val eventAId = UUID.fromString("00000000-0000-0000-0000-0000000000c1")
            val eventBId = UUID.fromString("00000000-0000-0000-0000-0000000000c2")
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventAId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 9),
                        entries = listOf(walletEntry(walletEventId = eventAId, walletItem = ownerAItem, value = "-20.00")),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventBId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 8),
                        entries = listOf(walletEntry(walletEventId = eventBId, walletItem = ownerBItem, value = "-25.00")),
                    ),
            )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            groupIds = setOf(groupId),
                            userIds = setOf(ownerA),
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items.mapNotNull { it.id }).containsExactly(eventAId)
        }

    @Test
    fun `should return all group events when userIds filter is empty`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val ownerA = UUID.randomUUID()
            val ownerB = UUID.randomUUID()
            val groupId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = setOf(groupId),
                    simulatedEvents = emptyList(),
                )

            val ownerAItem = bankAccountEntity(id = UUID.randomUUID(), userId = ownerA, name = "A")
            val ownerBItem = bankAccountEntity(id = UUID.randomUUID(), userId = ownerB, name = "B")
            listOf(ownerAItem, ownerBItem).forEach { walletItemRepository.save(it).block() }

            val eventAId = UUID.fromString("00000000-0000-0000-0000-0000000000d1")
            val eventBId = UUID.fromString("00000000-0000-0000-0000-0000000000d2")
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventAId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 10),
                        entries = listOf(walletEntry(walletEventId = eventAId, walletItem = ownerAItem, value = "-10.00")),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = eventBId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = requesterId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 9),
                        entries = listOf(walletEntry(walletEventId = eventBId, walletItem = ownerBItem, value = "-12.00")),
                    ),
            )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            groupIds = setOf(groupId),
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items.mapNotNull { it.id }).containsExactly(eventAId, eventBId)
        }

    @Test
    fun `should short-circuit when any requested group is unauthorized`() =
        runBlocking {
            val requesterId = UUID.randomUUID()
            val allowedGroupId = UUID.randomUUID()
            val deniedGroupId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = setOf(allowedGroupId),
                    simulatedEvents = emptyList(),
                )

            val result =
                service.list(
                    userId = requesterId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            groupIds = setOf(allowedGroupId, deniedGroupId),
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items).isEmpty()
            assertThat(result.hasNext).isFalse()
        }

    @Test
    fun `should apply multi-select filters with intersection semantics`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val bankId = UUID.randomUUID()
            val cardId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = setOf(groupId),
                    simulatedEvents = emptyList(),
                )

            val bankItem = bankAccountEntity(id = bankId, userId = userId, name = "Bank A")
            val cardItem = creditCardEntity(id = cardId, userId = userId, name = "Card A")
            walletItemRepository.save(bankItem).block()
            walletItemRepository.save(cardItem).block()

            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = userId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 10),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
                                    walletItem = bankItem,
                                    value = "-20.00",
                                ),
                            ),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = UUID.fromString("00000000-0000-0000-0000-0000000000a2"),
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = userId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 9),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = UUID.fromString("00000000-0000-0000-0000-0000000000a2"),
                                    walletItem = cardItem,
                                    value = "-30.00",
                                ),
                            ),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = UUID.fromString("00000000-0000-0000-0000-0000000000a3"),
                        type = WalletEntryType.REVENUE,
                        createdByUserId = userId,
                        groupId = groupId,
                        date = LocalDate.of(2026, 4, 8),
                        entries =
                            listOf(
                                walletEntry(
                                    walletEventId = UUID.fromString("00000000-0000-0000-0000-0000000000a3"),
                                    walletItem = cardItem,
                                    value = "60.00",
                                ),
                            ),
                    ),
            )

            val result =
                service.list(
                    userId = userId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            groupIds = setOf(groupId),
                            creditCardIds = setOf(cardId),
                            bankAccountIds = emptySet(),
                            entryTypes = setOf(WalletEntryType.EXPENSE),
                            pageRequest = CursorPageRequest(size = 20, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(result.items).hasSize(1)
            assertThat(result.items.single().id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-0000000000a2"))
            assertThat(result.hasNext).isFalse()
        }

    @Test
    fun `should keep cursor pagination stable across mixed-origin results`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val bankId = UUID.randomUUID()
            val cardId = UUID.randomUUID()
            val secondaryBankId = UUID.randomUUID()

            val walletItemRepository = InMemoryWalletItemRepository()
            val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
            val walletEntryRepository = InMemoryWalletEntryRepository()
            val service =
                createService(
                    walletEventRepository = walletEventRepository,
                    walletEntryRepository = walletEntryRepository,
                    walletItemRepository = walletItemRepository,
                    allowedGroupIds = emptySet(),
                    simulatedEvents = emptyList(),
                )

            val bankItem = bankAccountEntity(id = bankId, userId = userId, name = "Bank A")
            val cardItem = creditCardEntity(id = cardId, userId = userId, name = "Card A")
            val secondaryBankItem = bankAccountEntity(id = secondaryBankId, userId = userId, name = "Bank B")
            listOf(bankItem, cardItem, secondaryBankItem).forEach { walletItem -> walletItemRepository.save(walletItem).block() }

            val firstEventId = UUID.fromString("00000000-0000-0000-0000-000000000003")
            val secondEventId = UUID.fromString("00000000-0000-0000-0000-000000000002")
            val thirdEventId = UUID.fromString("00000000-0000-0000-0000-000000000001")
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = firstEventId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = userId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 10),
                        entries = listOf(walletEntry(walletEventId = firstEventId, walletItem = bankItem, value = "-10.00")),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = secondEventId,
                        type = WalletEntryType.EXPENSE,
                        createdByUserId = userId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 10),
                        entries = listOf(walletEntry(walletEventId = secondEventId, walletItem = cardItem, value = "-30.00")),
                    ),
            )
            saveEvent(
                repository = walletEventRepository,
                event =
                    walletEvent(
                        id = thirdEventId,
                        type = WalletEntryType.REVENUE,
                        createdByUserId = userId,
                        groupId = null,
                        date = LocalDate.of(2026, 4, 9),
                        entries = listOf(walletEntry(walletEventId = thirdEventId, walletItem = secondaryBankItem, value = "120.00")),
                    ),
            )

            val firstPage =
                service.list(
                    userId = userId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            pageRequest = CursorPageRequest(size = 2, nextCursor = mapOf("skipFuture" to true)),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(firstPage.items.mapNotNull { it.id }).containsExactly(firstEventId, secondEventId)
            assertThat(firstPage.hasNext).isTrue()

            val secondPage =
                service.list(
                    userId = userId,
                    request =
                        ListEntryRequest(
                            walletItemId = null,
                            pageRequest =
                                CursorPageRequest(
                                    size = 2,
                                    nextCursor = (firstPage.nextCursor ?: emptyMap()) + mapOf("skipFuture" to true),
                                ),
                            minimumDate = LocalDate.of(2026, 4, 1),
                            maximumDate = LocalDate.of(2026, 4, 30),
                            billId = null,
                            billDate = null,
                        ),
                )

            assertThat(secondPage.items.mapNotNull { it.id }).containsExactly(thirdEventId)
            assertThat(secondPage.hasNext).isFalse()

            val mergedIds = (firstPage.items + secondPage.items).mapNotNull { it.id }
            assertThat(mergedIds).containsExactly(firstEventId, secondEventId, thirdEventId)
            assertThat(mergedIds.toSet()).hasSize(3)
        }

    private fun createService(
        walletEventRepository: InMemoryWalletEventRepository,
        walletEntryRepository: InMemoryWalletEntryRepository,
        walletItemRepository: InMemoryWalletItemRepository,
        allowedGroupIds: Set<UUID>,
        simulatedEvents: List<EventListResponse>,
    ): WalletEventListServiceImpl =
        WalletEventListServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            recurrenceEntryRepository = InMemoryRecurrenceEntryRepository(),
            creditCardBillRepository = InMemoryCreditCardBillRepository(walletItemRepository),
            walletItemMapper = WalletItemMapperImpl(BankAccountMapperImpl(), CreditCardMapperImpl()),
            walletItemService = noOpWalletItemService(),
            genericCategoryService = noOpCategoryService(),
            groupService = noOpGroupService(),
            groupPermissionService = allowListedGroupPermissionService(allowedGroupIds),
            userService = noOpUserService(),
            recurrenceSeriesRepository = InMemoryRecurrenceSeriesRepository(),
            recurrenceSimulationService = recurrenceSimulationService(simulatedEvents),
            recurrenceService = noOpRecurrenceService(),
        )

    private fun saveEvent(
        repository: InMemoryWalletEventRepository,
        event: WalletEventEntity,
    ) {
        repository.save(event).block()
    }

    private fun walletEvent(
        id: UUID,
        type: WalletEntryType,
        createdByUserId: UUID,
        groupId: UUID?,
        date: LocalDate,
        entries: List<WalletEntryEntity>,
    ): WalletEventEntity =
        WalletEventEntity(
            type = type,
            name = "event-$id",
            categoryId = null,
            createdByUserId = createdByUserId,
            groupId = groupId,
            tags = emptyList(),
            observations = null,
            date = date,
            confirmed = true,
            installment = null,
            recurrenceEventId = null,
            paymentType = PaymentType.UNIQUE,
        ).also { event ->
            event.id = id
            entries.forEach { entry -> entry.event = event }
            event.entries = entries
        }

    private fun walletEntry(
        walletEventId: UUID,
        walletItem: WalletItemEntity,
        value: String,
    ): WalletEntryEntity =
        WalletEntryEntity(
            value = BigDecimal(value),
            walletEventId = walletEventId,
            walletItemId = walletItem.id!!,
            billId = null,
        ).also { entry ->
            entry.id = UUID.randomUUID()
            entry.walletItem = walletItem
        }

    private fun bankAccountEntity(
        id: UUID,
        userId: UUID,
        name: String,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = name,
            enabled = true,
            userId = userId,
            currency = "BRL",
            balance = BigDecimal("1000.00"),
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
            showOnDashboard = true,
        ).also { it.id = id }

    private fun creditCardEntity(
        id: UUID,
        userId: UUID,
        name: String,
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.CREDIT_CARD,
            name = name,
            enabled = true,
            userId = userId,
            currency = "BRL",
            balance = BigDecimal("0.00"),
            totalLimit = BigDecimal("5000.00"),
            dueDay = 10,
            daysBetweenDueAndClosing = 7,
            dueOnNextBusinessDay = true,
            showOnDashboard = true,
        ).also { it.id = id }

    private fun allowListedGroupPermissionService(allowedGroupIds: Set<UUID>): GroupPermissionService =
        object : GroupPermissionService {
            override suspend fun hasPermission(
                userId: UUID,
                groupId: UUID,
                permission: GroupPermissions?,
            ): Boolean = allowedGroupIds.contains(groupId)

            override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
        }

    private fun noOpCategoryService(): GenericCategoryService =
        object : GenericCategoryService {
            override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletEntryCategoryEntity> = emptyFlow()
        }

    private fun noOpGroupService(): GroupService =
        object : GroupService {
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
            ): GroupWithRole? = null

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
            ): GroupEntity = GroupEntity(name = "noop").also { it.id = UUID.randomUUID() }

            override suspend fun findAllMembers(
                userId: UUID,
                id: UUID,
            ): List<GroupUserEntity> = emptyList()

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

    private fun noOpUserService(): UserService =
        object : UserService {
            override suspend fun createUser(request: RegisterDto): UserEntity = error("not used")

            override suspend fun changeLanguage(
                userId: UUID,
                newLang: String,
            ) = Unit

            override fun findAllByIdIn(ids: Collection<UUID>): Flow<UserEntity> = emptyFlow()

            override suspend fun updateUser(
                userId: UUID,
                updateUserDto: UpdateUserDto,
                newAvatar: FilePart?,
            ): UserEntity = error("not used")

            override suspend fun changePassword(
                userId: UUID,
                currentPasswordHash: String?,
                newPasswordHash: String,
            ) = Unit

            override suspend fun deleteCurrentAccount(userId: UUID) = Unit
        }

    private fun noOpWalletItemService(): WalletItemService =
        object : WalletItemService {
            override suspend fun findAllItems(
                userId: UUID,
                pageable: Pageable,
                onlyBankAccounts: Boolean,
            ): Page<WalletItem> = error("not used")

            override suspend fun findOne(id: UUID): WalletItem? = null

            override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletItem> = emptyFlow()

            override suspend fun addBalanceById(
                id: UUID,
                balance: BigDecimal,
            ): Long = 0
        }

    private fun noOpRecurrenceService(): RecurrenceService =
        object : RecurrenceService {
            override fun findAllByIdIn(ids: Collection<UUID>): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun findAllEntryByWalletId(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                billDate: LocalDate?,
                walletItemId: UUID,
                userId: UUID?,
                groupId: UUID?,
                sort: Sort,
            ): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun findAllEntryByUserId(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID,
                sort: Sort,
            ): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun findAllEntryByUserIds(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userIds: Set<UUID>,
                sort: Sort,
            ): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun findAllEntryByGroupId(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                groupId: UUID,
                sort: Sort,
            ): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun findAllEntries(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                billDate: LocalDate?,
                walletItemId: UUID?,
                walletItemIds: Set<UUID>,
                userIds: Set<UUID>,
                groupIds: Set<UUID>,
                entryTypes: Set<WalletEntryType>,
                sort: Sort,
            ): Flow<RecurrenceEventEntity> = emptyFlow()

            override fun calculateNextExecution(
                lastExecution: LocalDate,
                periodicity: RecurrenceType,
                qtyExecuted: Int,
                qtyLimit: Int?,
            ): LocalDate? = null

            override fun calculateEndDate(
                lastExecution: LocalDate,
                periodicity: RecurrenceType,
                qtyExecuted: Int,
                qtyLimit: Int?,
            ): LocalDate? = null

            override fun calculateNextDate(
                lastExecution: LocalDate,
                periodicity: RecurrenceType,
            ): LocalDate = lastExecution

            override suspend fun queueAllPendingOfExecution(): Int = 0
        }

    private fun recurrenceSimulationService(events: List<EventListResponse>): RecurrenceSimulationService =
        object : RecurrenceSimulationService {
            override suspend fun getFutureValuesOfWalletItem(
                walletItemId: UUID,
                minimumEndExecution: LocalDate,
                maximumNextExecution: LocalDate,
                userId: UUID,
                groupId: UUID?,
            ): BigDecimal = BigDecimal.ZERO

            override suspend fun getFutureValuesOCreditCard(
                bill: CreditCardBill,
                userId: UUID,
                groupId: UUID?,
                walletItemId: UUID,
            ): BigDecimal = BigDecimal.ZERO

            override suspend fun simulateGeneration(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemId: UUID?,
                billDate: LocalDate?,
            ): List<EventListResponse> =
                events.filter {
                    (minimumEndExecution == null || !it.date.isBefore(minimumEndExecution)) &&
                        (maximumNextExecution == null || !it.date.isAfter(maximumNextExecution))
                }

            override suspend fun simulateGenerationForUsers(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userIds: Set<UUID>,
                billDate: LocalDate?,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGenerationAsEntrySumResult(
                minimumEndExecution: LocalDate?,
                maximumNextExecution: LocalDate?,
                userId: UUID?,
                groupId: UUID?,
                walletItemId: UUID?,
                summaryMinimumDate: LocalDate,
            ): List<EntrySumResult> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                billDate: LocalDate,
                userId: UUID,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemId: UUID,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGenerationForCreditCard(
                bill: CreditCardBill,
                userId: UUID,
                groupIds: Set<UUID>,
                userIds: Set<UUID>,
                walletItemId: UUID?,
            ): List<EventListResponse> = emptyList()

            override suspend fun simulateGeneration(
                config: RecurrenceEventEntity,
                walletItems: List<WalletItem>,
                user: UserEntity?,
                group: GroupEntity?,
                category: WalletEntryCategoryEntity?,
                simulateBillForRecurrence: Boolean,
            ): EventListResponse = error("not used")
        }

    @Suppress("unused")
    private fun sampleUser(id: UUID): UserEntity =
        UserEntity(
            email = "user@example.com",
            passwordHash = null,
            firstName = "User",
            lastName = "Test",
            lang = "pt-BR",
            defaultCurrency = "BRL",
            tmz = "America/Sao_Paulo",
            photoUrl = null,
            emailVerified = true,
            mfaEnabled = false,
            totpSecret = null,
            onboardingDone = true,
            termsAcceptedAt = OffsetDateTime.now(),
            termsVersion = "1",
            privacyAcceptedAt = OffsetDateTime.now(),
            privacyVersion = "1",
        ).also { it.id = id }
}
