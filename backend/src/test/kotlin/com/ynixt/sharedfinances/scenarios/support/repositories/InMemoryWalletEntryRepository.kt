package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class InMemoryWalletEntryRepository(
    private val walletEventRepository: InMemoryWalletEventRepository? = null,
    private val walletItemRepository: InMemoryWalletItemRepository? = null,
) : WalletEntryRepository {
    private val data = linkedMapOf<UUID, WalletEntryEntity>()

    fun deleteAllByWalletEventIds(walletEventIds: Set<UUID>): Int {
        val initial = data.size
        data.entries.removeIf { (_, entry) -> walletEventIds.contains(entry.walletEventId) }
        return initial - data.size
    }

    override fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEntryEntity> =
        Flux.fromIterable(data.values.filter { it.walletEventId == walletEventId })

    override fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int> {
        val initial = data.size
        data.entries.removeIf { (_, entry) -> entry.walletEventId == walletEventId }
        return Mono.just(initial - data.size)
    }

    override fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
        asOfDate: LocalDate,
    ): Flux<EntrySumResult> = Flux.empty()

    override fun summarizeBankAccountsByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<BankAccountMonthlySummary> {
        val support = buildSupport() ?: return Flux.empty()
        val summaries = mutableMapOf<Pair<UUID, YearMonth>, Triple<BigDecimal, BigDecimal, BigDecimal>>()

        data.values.forEach { entry ->
            val walletItem = support.itemById[entry.walletItemId] ?: return@forEach
            val event = support.eventById[entry.walletEventId] ?: return@forEach
            if (walletItem.userId != userId ||
                walletItem.type != WalletItemType.BANK_ACCOUNT ||
                !walletItem.enabled ||
                !walletItem.showOnDashboard ||
                event.date.isBefore(minimumDate) ||
                event.date.isAfter(maximumDate)
            ) {
                return@forEach
            }

            val month = YearMonth.from(event.date)
            val key = entry.walletItemId to month
            val current = summaries[key] ?: Triple(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            val internalTransfer = support.isInternalBankTransfer(entry = entry, event = event, userId = userId)
            val cashIn = if (entry.value > BigDecimal.ZERO && !event.initialBalance && !internalTransfer) entry.value else BigDecimal.ZERO
            val cashOut =
                if (entry.value < BigDecimal.ZERO &&
                    !event.initialBalance &&
                    !internalTransfer
                ) {
                    entry.value.abs()
                } else {
                    BigDecimal.ZERO
                }

            summaries[key] =
                Triple(
                    current.first.add(entry.value),
                    current.second.add(cashIn),
                    current.third.add(cashOut),
                )
        }

        return Flux.fromIterable(
            summaries.entries
                .sortedWith(compareBy({ it.key.second }, { it.key.first }))
                .map { (key, value) ->
                    BankAccountMonthlySummary(
                        walletItemId = key.first,
                        month = key.second,
                        net = value.first,
                        cashIn = value.second,
                        cashOut = value.third,
                    )
                },
        )
    }

    override fun summarizeOverviewExpenseByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseMonthlySummary> {
        val support = buildSupport() ?: return Flux.empty()
        val summaries = mutableMapOf<Pair<YearMonth, String>, BigDecimal>()

        data.values.forEach { entry ->
            val walletItem = support.itemById[entry.walletItemId] ?: return@forEach
            val event = support.eventById[entry.walletEventId] ?: return@forEach
            if (walletItem.userId != userId ||
                walletItem.type !in setOf(WalletItemType.BANK_ACCOUNT, WalletItemType.CREDIT_CARD) ||
                !walletItem.enabled ||
                !walletItem.showOnDashboard ||
                event.initialBalance ||
                event.type != WalletEntryType.EXPENSE ||
                entry.value >= BigDecimal.ZERO ||
                event.date.isBefore(minimumDate) ||
                event.date.isAfter(maximumDate)
            ) {
                return@forEach
            }

            val key = YearMonth.from(event.date) to walletItem.currency
            summaries[key] = summaries.getOrDefault(key, BigDecimal.ZERO).add(entry.value.abs())
        }

        return Flux.fromIterable(
            summaries.entries
                .sortedWith(compareBy({ it.key.first }, { it.key.second }))
                .map { (key, expense) ->
                    OverviewExpenseMonthlySummary(
                        month = key.first,
                        currency = key.second,
                        expense = expense,
                    )
                },
        )
    }

    override fun summarizeOverviewCashBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewCashBreakdownSummary> {
        val support = buildSupport() ?: return Flux.empty()
        val summaries = mutableMapOf<Triple<OverviewCashDirection, UUID?, String>, BigDecimal>()

        data.values.forEach { entry ->
            val walletItem = support.itemById[entry.walletItemId] ?: return@forEach
            val event = support.eventById[entry.walletEventId] ?: return@forEach
            if (walletItem.userId != userId ||
                walletItem.type != WalletItemType.BANK_ACCOUNT ||
                !walletItem.enabled ||
                !walletItem.showOnDashboard ||
                event.initialBalance ||
                event.date.isBefore(minimumDate) ||
                event.date.isAfter(maximumDate) ||
                support.isInternalBankTransfer(entry = entry, event = event, userId = userId)
            ) {
                return@forEach
            }

            val direction = if (entry.value >= BigDecimal.ZERO) OverviewCashDirection.IN else OverviewCashDirection.OUT
            val key = Triple(direction, event.categoryId, walletItem.currency)
            summaries[key] = summaries.getOrDefault(key, BigDecimal.ZERO).add(entry.value.abs())
        }

        return Flux.fromIterable(
            summaries.entries
                .map { (key, amount) ->
                    OverviewCashBreakdownSummary(
                        direction = key.first,
                        categoryId = key.second,
                        categoryName = null,
                        currency = key.third,
                        amount = amount,
                    )
                },
        )
    }

    override fun summarizeOverviewExpenseBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseBreakdownSummary> {
        val support = buildSupport() ?: return Flux.empty()
        val summaries = mutableMapOf<Triple<UUID?, UUID?, String>, BigDecimal>()

        data.values.forEach { entry ->
            val walletItem = support.itemById[entry.walletItemId] ?: return@forEach
            val event = support.eventById[entry.walletEventId] ?: return@forEach
            if (walletItem.userId != userId ||
                walletItem.type !in setOf(WalletItemType.BANK_ACCOUNT, WalletItemType.CREDIT_CARD) ||
                !walletItem.enabled ||
                !walletItem.showOnDashboard ||
                event.initialBalance ||
                event.type != WalletEntryType.EXPENSE ||
                entry.value >= BigDecimal.ZERO ||
                event.date.isBefore(minimumDate) ||
                event.date.isAfter(maximumDate)
            ) {
                return@forEach
            }

            val key = Triple(event.groupId, event.categoryId, walletItem.currency)
            summaries[key] = summaries.getOrDefault(key, BigDecimal.ZERO).add(entry.value.abs())
        }

        return Flux.fromIterable(
            summaries.entries.map { (key, expense) ->
                OverviewExpenseBreakdownSummary(
                    groupId = key.first,
                    groupName = null,
                    categoryId = key.second,
                    categoryName = null,
                    currency = key.third,
                    expense = expense,
                )
            },
        )
    }

    override fun findById(id: UUID): Mono<WalletEntryEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : WalletEntryEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : WalletEntryEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<WalletEntryEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun buildSupport(): Support? {
        val events = walletEventRepository ?: return null
        val items = walletItemRepository ?: return null
        return Support(
            eventById = events.snapshot().associateBy { it.id!! },
            entriesByEventId = data.values.groupBy { it.walletEventId },
            itemById = items.snapshot().associateBy { it.id!! },
        )
    }

    private data class Support(
        val eventById: Map<UUID, com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity>,
        val entriesByEventId: Map<UUID, List<WalletEntryEntity>>,
        val itemById: Map<UUID, com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity>,
    ) {
        fun isInternalBankTransfer(
            entry: WalletEntryEntity,
            event: com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity,
            userId: UUID,
        ): Boolean {
            if (event.type != WalletEntryType.TRANSFER) {
                return false
            }

            val counterpartItems =
                entriesByEventId
                    .getOrDefault(entry.walletEventId, emptyList())
                    .filter { it.id != entry.id }
                    .mapNotNull { itemById[it.walletItemId] }

            return counterpartItems.any { it.type == WalletItemType.BANK_ACCOUNT && it.userId == userId }
        }
    }
}
