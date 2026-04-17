package com.ynixt.sharedfinances.scenarios.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

internal class InMemoryWalletItemRepository : WalletItemRepository {
    private val data = linkedMapOf<UUID, WalletItemEntity>()

    fun snapshot(): List<WalletItemEntity> = data.values.toList()

    fun getOrNull(id: UUID): WalletItemEntity? = data[id]

    override fun findDistinctCurrencies(): Flux<String> = Flux.fromIterable(data.values.map { it.currency }.distinct())

    override fun findAllByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemEntity> = Flux.fromIterable(page(filterItems { it.userId == userId && it.enabled == enabled }, pageable))

    override fun findAllByUserIdAndEnabledAndShowOnDashboard(
        userId: UUID,
        enabled: Boolean,
        showOnDashboard: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemEntity> =
        Flux.fromIterable(
            page(
                filterItems {
                    it.userId == userId &&
                        it.enabled == enabled &&
                        it.showOnDashboard == showOnDashboard
                },
                pageable,
            ),
        )

    override fun countByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
    ): Mono<Long> = Mono.just(filterItems { it.userId == userId && it.enabled == enabled }.size.toLong())

    override fun findAllByUserIdAndType(
        userId: UUID,
        type: WalletItemType,
        pageable: Pageable,
    ): Flux<WalletItemEntity> = Flux.fromIterable(page(filterItems { it.userId == userId && it.type == type }, pageable))

    override fun countByUserIdAndType(
        userId: UUID,
        type: WalletItemType,
    ): Mono<Long> = Mono.just(filterItems { it.userId == userId && it.type == type }.size.toLong())

    override fun findAllByUserIdAndTypeAndNameContainingIgnoreCase(
        userId: UUID,
        type: WalletItemType,
        name: String,
        pageable: Pageable,
    ): Flux<WalletItemEntity> =
        Flux.fromIterable(
            page(
                filterItems {
                    it.userId == userId &&
                        it.type == type &&
                        it.name.lowercase().contains(name.lowercase())
                },
                pageable,
            ),
        )

    override fun countByUserIdAndTypeAndNameContainingIgnoreCase(
        userId: UUID,
        type: WalletItemType,
        name: String,
    ): Mono<Long> =
        Mono.just(
            filterItems {
                it.userId == userId &&
                    it.type == type &&
                    it.name.lowercase().contains(name.lowercase())
            }.size.toLong(),
        )

    override fun findOneById(id: UUID): Mono<WalletItemEntity> = Mono.justOrEmpty(data[id])

    override fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<WalletItemEntity> = Mono.justOrEmpty(data[id]?.takeIf { it.userId == userId })

    override fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long> {
        val current = data[id]
        return if (current != null && current.userId == userId) {
            data.remove(id)
            Mono.just(1L)
        } else {
            Mono.just(0L)
        }
    }

    override fun updateBankAccount(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newShowOnDashboard: Boolean,
    ): Mono<Long> {
        val current = data[id]
        if (current == null || current.userId != userId || current.type != WalletItemType.BANK_ACCOUNT) {
            return Mono.just(0L)
        }

        data[id] =
            copyWalletItem(
                current = current,
                name = newName,
                enabled = newEnabled,
                currency = newCurrency,
                showOnDashboard = newShowOnDashboard,
            )
        return Mono.just(1L)
    }

    override fun updateCreditCard(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newShowOnDashboard: Boolean,
        newTotalLimit: BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long> {
        val current = data[id]
        if (current == null || current.userId != userId || current.type != WalletItemType.CREDIT_CARD) {
            return Mono.just(0L)
        }

        data[id] =
            copyWalletItem(
                current = current,
                name = newName,
                enabled = newEnabled,
                currency = newCurrency,
                showOnDashboard = newShowOnDashboard,
                totalLimit = newTotalLimit,
                dueDay = newDueDay,
                daysBetweenDueAndClosing = newDaysBetweenDueAndClosing,
                dueOnNextBusinessDay = newDueOnNextBusinessDay,
            )
        return Mono.just(1L)
    }

    override fun addBalanceById(
        id: UUID,
        balance: BigDecimal,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        data[id] = copyWalletItem(current = current, balance = current.balance.add(balance))
        return Mono.just(1L)
    }

    override fun findById(id: UUID): Mono<WalletItemEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : WalletItemEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : WalletItemEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<WalletItemEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun filterItems(predicate: (WalletItemEntity) -> Boolean): List<WalletItemEntity> = data.values.filter(predicate)

    private fun page(
        items: List<WalletItemEntity>,
        pageable: Pageable,
    ): List<WalletItemEntity> {
        if (pageable.isUnpaged) {
            return items
        }

        val offset = pageable.offset.toInt()
        val endExclusive = (offset + pageable.pageSize).coerceAtMost(items.size)
        if (offset >= items.size) return emptyList()
        return items.subList(offset, endExclusive)
    }

    private fun copyWalletItem(
        current: WalletItemEntity,
        name: String = current.name,
        enabled: Boolean = current.enabled,
        currency: String = current.currency,
        balance: BigDecimal = current.balance,
        showOnDashboard: Boolean = current.showOnDashboard,
        totalLimit: BigDecimal? = current.totalLimit,
        dueDay: Int? = current.dueDay,
        daysBetweenDueAndClosing: Int? = current.daysBetweenDueAndClosing,
        dueOnNextBusinessDay: Boolean? = current.dueOnNextBusinessDay,
    ): WalletItemEntity =
        WalletItemEntity(
            type = current.type,
            name = name,
            enabled = enabled,
            userId = current.userId,
            currency = currency,
            balance = balance,
            totalLimit = totalLimit,
            dueDay = dueDay,
            daysBetweenDueAndClosing = daysBetweenDueAndClosing,
            dueOnNextBusinessDay = dueOnNextBusinessDay,
            showOnDashboard = showOnDashboard,
        ).also {
            it.id = current.id
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
        }
}
