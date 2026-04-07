package com.ynixt.sharedfinances.scenario.support.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.scenario.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class InMemoryCreditCardBillRepository(
    private val walletItemRepository: InMemoryWalletItemRepository,
) : CreditCardBillRepository {
    private val data = linkedMapOf<UUID, CreditCardBillEntity>()

    override fun findOneByCreditCardIdAndBillDate(
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity> =
        Mono.justOrEmpty(
            data.values.firstOrNull {
                it.creditCardId == creditCardId && it.billDate == billDate
            },
        )

    override fun findOneByUserIdAndCreditCardIdAndBillDate(
        userId: UUID,
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity> =
        findOneByCreditCardIdAndBillDate(creditCardId, billDate).filterWhen { bill ->
            walletItemRepository
                .findById(bill.creditCardId)
                .map { item -> item.userId == userId }
        }

    override fun addValueById(
        id: UUID,
        value: BigDecimal,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        data[id] = copyBill(current, newValue = current.value.add(value))
        return Mono.just(1L)
    }

    override fun changeDueDateById(
        id: UUID,
        dueDate: LocalDate,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        data[id] = copyBill(current, dueDate = dueDate)
        return Mono.just(1L)
    }

    override fun changeClosingDateById(
        id: UUID,
        closingDate: LocalDate,
    ): Mono<Long> {
        val current = data[id] ?: return Mono.just(0L)
        data[id] = copyBill(current, closingDate = closingDate)
        return Mono.just(1L)
    }

    override fun findById(id: UUID): Mono<CreditCardBillEntity> = Mono.justOrEmpty(data[id])

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(if (data.remove(id) != null) 1L else 0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(data.containsKey(id))

    override fun <S : CreditCardBillEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        data[id] = entity
        return Mono.just(entity)
    }

    override fun <S : CreditCardBillEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<CreditCardBillEntity> = Flux.fromIterable(id.mapNotNull { data[it] })

    private fun copyBill(
        current: CreditCardBillEntity,
        dueDate: LocalDate = current.dueDate,
        closingDate: LocalDate = current.closingDate,
        newValue: BigDecimal = current.value,
    ): CreditCardBillEntity =
        CreditCardBillEntity(
            creditCardId = current.creditCardId,
            billDate = current.billDate,
            dueDate = dueDate,
            closingDate = closingDate,
            paid = current.paid,
            value = newValue,
        ).also {
            it.id = current.id
            it.createdAt = current.createdAt
            it.updatedAt = nowOffset()
        }
}
