package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
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
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
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
    ): Mono<MinimumWalletEntry> =
        loadRelationships(userId, newEntryRequest).flatMap { newEntryRequest ->
            checkDataIntegrity(newEntryRequest)
            if (checkAllPermissions(userId, newEntryRequest)) {
                createWithoutCheckPermissions(userId, newEntryRequest)
                    .flatMap { walletEntryActionEventService.sendInsertedWalletEntry(userId, it).thenReturn(it) }
            } else {
                Mono.empty()
            }
        }

    private fun updateBalance(newEntryRequest: NewEntryRequest): Mono<Void> {
        val valueForOrigin = newEntryRequest.valueFixedForType
        val valueForTarget = newEntryRequest.value.abs()

        val updateOriginBalance =
            newEntryRequest.origin!!.let { origin ->
                val valueForOrigin = valueForOrigin

                walletItemService.addBalanceById(origin.id!!, valueForOrigin)
            }

        val updateOriginalBillValue =
            newEntryRequest.originBill?.let { originBill ->
                creditCardBillService.addValueById(
                    originBill.id!!,
                    valueForOrigin,
                )
            } ?: Mono.empty()

        val updateTargetBalance =
            newEntryRequest.target?.let { origin ->
                walletItemService.addBalanceById(origin.id!!, valueForTarget)
            } ?: Mono.empty()

        val updateTargetBillValue =
            newEntryRequest.targetBill?.let { targetBill ->
                creditCardBillService.addValueById(
                    targetBill.id!!,
                    valueForTarget,
                )
            } ?: Mono.empty()

        return Mono
            .zip(
                updateOriginBalance.defaultIfEmpty(0),
                updateTargetBalance.defaultIfEmpty(0),
                updateOriginalBillValue.defaultIfEmpty(0),
                updateTargetBillValue.defaultIfEmpty(0),
            ).then()
    }

    private fun createWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<MinimumWalletEntry> {
        val recurrenceConfigMono = createRecurrenceConfig(userId, newEntryRequest)

        return if (newEntryRequest.inFuture) recurrenceConfigMono.map { it.value!! }
        else {
            recurrenceConfigMono.flatMap { recurrenceConfig ->
                val installments = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) newEntryRequest.installments!! else 1

                if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
                    var date: LocalDate = newEntryRequest.date

                    walletEntryRepository
                        .saveAll(
                            (1..installments).map {
                                date =
                                    when (newEntryRequest.periodicity!!) {
                                        RecurrenceType.SINGLE -> throw IllegalArgumentException("Single recurrence is not allowed here.")
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
            }.flatMap {
                updateBalance(newEntryRequest).thenReturn(it)
            }
        }
    }

    private fun createRecurrenceConfig(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Mono<Wrapper<EntryRecurrenceConfigEntity>> {
        return when (newEntryRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                entryRecurrenceConfigRepository
                    .save(
                        requestToRecurrenceEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            qtyLimit = newEntryRequest.installments!!,
                            qtyExecuted = if (newEntryRequest.inFuture) 0 else newEntryRequest.installments,
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
                            qtyExecuted = if (newEntryRequest.inFuture) 0 else 1,
                        ),
                    ).map { Wrapper(it) }

            else -> {
                if (newEntryRequest.inFuture)
                    entryRecurrenceConfigRepository
                        .save(
                            requestToRecurrenceEntity(
                                id = null,
                                userId = userId,
                                newEntryRequest = newEntryRequest,
                                qtyLimit = 1,
                                qtyExecuted = 0,
                            ),
                        ).map { Wrapper(it) }
                else Mono.just(Wrapper.empty())
            }
        }
    }
}
