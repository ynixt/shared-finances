package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.Wrapper
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.EntryRecurrenceService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEntryActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEntryCreateServiceImpl(
    private val walletEntryRepository: WalletEntryRepository,
    groupService: GroupService,
    walletItemService: WalletItemService,
    creditCardBillService: CreditCardBillService,
    entryRecurrenceService: EntryRecurrenceService,
    private val entryRecurrenceConfigRepository: EntryRecurrenceConfigRepository,
    private val walletEntryActionEventService: WalletEntryActionEventService,
) : WalletEntrySaveServiceImpl(
        groupService = groupService,
        walletItemService = walletItemService,
        creditCardBillService = creditCardBillService,
        entryRecurrenceService = entryRecurrenceService,
    ),
    WalletEntryCreateService {
    @Transactional
    override fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<WalletEntryEntity> =
        loadRelationships(userId, newEntryRequest).flatMap { newEntryRequest ->
            checkDataIntegrity(newEntryRequest)
            if (checkAllPermissions(userId, newEntryRequest)) {
                createWithoutCheckPermissions(userId, newEntryRequest)
                    .flatMap { updateBalance(newEntryRequest).thenReturn(it) }
                    .flatMap { walletEntryActionEventService.sendInsertedWalletEntry(userId, it).thenReturn(it) }
            } else {
                Mono.empty()
            }
        }

    private fun updateBalance(newEntryRequest: NewEntryRequest): Mono<Void> {
        val updateOriginBalance =
            newEntryRequest.origin!!.let { origin ->
                val valueForOrigin =
                    if (newEntryRequest.type == WalletEntryType.TRANSFER || newEntryRequest.type == WalletEntryType.EXPENSE) {
                        newEntryRequest.value.unaryMinus()
                    } else {
                        newEntryRequest.value.abs()
                    }

                walletItemService.addBalanceById(origin.id!!, valueForOrigin)
            }

        val updateTargetBalance =
            newEntryRequest.target?.let { origin ->
                walletItemService.addBalanceById(origin.id!!, newEntryRequest.value.abs())
            } ?: Mono.empty()

        return Mono
            .zip(
                updateOriginBalance,
                updateTargetBalance,
            ).then()
    }

    private fun createWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<WalletEntryEntity> {
        val recurrenceConfigMono = createRecurrenceConfig(userId, newEntryRequest)

        return recurrenceConfigMono.flatMap { recurrenceConfig ->
            val installments = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) newEntryRequest.installments!! else 1

            if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
                var date: LocalDate = newEntryRequest.date

                walletEntryRepository
                    .saveAll(
                        (1..installments).map {
                            date =
                                when (newEntryRequest.periodicity!!) {
                                    RecurrenceType.DAILY -> date.plusDays(it.toLong() - 1)
                                    RecurrenceType.WEEKLY -> date.plusWeeks(it.toLong() - 1)
                                    RecurrenceType.MONTHLY -> date.plusMonths(it.toLong() - 1)
                                    RecurrenceType.YEARLY -> date.plusYears(it.toLong() - 1)
                                }

                            requestToEntity(
                                id = null,
                                userId = userId,
                                newEntryRequest = newEntryRequest,
                                recurrenceConfig = recurrenceConfig.value,
                                installment = it,
                                date = date,
                            )
                        },
                    ).collectList()
                    .flatMap {
                        Mono.just(it.first())
                    }
            } else {
                walletEntryRepository.save(
                    requestToEntity(
                        id = null,
                        userId = userId,
                        newEntryRequest = newEntryRequest,
                        recurrenceConfig = recurrenceConfig.value,
                        installment = null,
                        date = newEntryRequest.date,
                    ),
                )
            }
        }
    }

    private fun createRecurrenceConfig(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<Wrapper<EntryRecurrenceConfigEntity>> =
        when (newEntryRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                entryRecurrenceConfigRepository
                    .save(
                        requestToRecurrenceEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            qtyLimit = newEntryRequest.installments!!,
                            qtyExecuted = newEntryRequest.installments,
                        ),
                    ).map { Wrapper(it) }

            PaymentType.RECURRING ->
                entryRecurrenceConfigRepository
                    .save(
                        requestToRecurrenceEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            qtyLimit = newEntryRequest.periodicityQtyLimit,
                            qtyExecuted = 1,
                        ),
                    ).map { Wrapper(it) }

            else -> Mono.just(Wrapper.empty())
        }
}
