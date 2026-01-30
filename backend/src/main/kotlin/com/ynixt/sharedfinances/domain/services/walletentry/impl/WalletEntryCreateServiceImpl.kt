package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.Wrapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
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
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.ifEmpty

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

    @Transactional
    override fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): Mono<MinimumWalletEntry> =
        entryRecurrenceConfigRepository.findById(recurrenceConfigId).flatMap { config ->
            entryRecurrenceConfigRepository
                .updateConfigCausedByExecution(
                    id = recurrenceConfigId,
                    oldNextExecution = date,
                    nextExecution =
                        entryRecurrenceService.calculateNextDate(
                            lastExecution = date,
                            periodicity = config.periodicity,
                            qtyExecuted = config.qtyExecuted,
                            qtyLimit = config.qtyLimit,
                        ),
                ).flatMap { linesModified ->
                    if (linesModified == 0 || config.userId == null) {
                        Mono.empty()
                    } else {
                        val executionDate = config.nextExecution!!

                        walletItemService
                            .findAllByIdIn(
                                listOfNotNull(config.originId, config.targetId).distinct(),
                            ).collectList()
                            .flatMap { walletItems ->
                                val origin = walletItems.find { it.id == config.originId }!!
                                val target = walletItems.find { it.id == config.targetId }

                                val originBillMono =
                                    if (origin is CreditCard) {
                                        val billDate = origin.getBestBill(executionDate)
                                        val dueDate = origin.getDueDate(billDate)
                                        creditCardBillService
                                            .getOrCreateBill(
                                                creditCardId = origin.id!!,
                                                dueDate = dueDate,
                                                closingDate = origin.getClosingDate(dueDate),
                                            ).map { Wrapper(it) }
                                    } else {
                                        Mono.just(Wrapper(null))
                                    }

                                val targetBillMono =
                                    if (target != null && target is CreditCard) {
                                        val billDate = target.getBestBill(executionDate)
                                        val dueDate = target.getDueDate(billDate)
                                        creditCardBillService
                                            .getOrCreateBill(
                                                creditCardId = target.id!!,
                                                dueDate = dueDate,
                                                closingDate = target.getClosingDate(dueDate),
                                            ).map { Wrapper(it) }
                                    } else {
                                        Mono.just(Wrapper(null))
                                    }

                                Mono
                                    .zip(originBillMono, targetBillMono)
                                    .flatMap { (originBillWrapper, targetBillWrapper) ->
                                        val originBill = originBillWrapper.value
                                        val targetBill = targetBillWrapper.value

                                        val installment =
                                            if (config.paymentType ==
                                                PaymentType.INSTALLMENTS
                                            ) {
                                                config.qtyExecuted + 1
                                            } else {
                                                null
                                            }

                                        walletEntryRepository
                                            .save(
                                                WalletEntryEntity(
                                                    type = config.type,
                                                    userId = config.userId,
                                                    groupId = config.groupId,
                                                    originId = config.originId,
                                                    targetId = config.targetId,
                                                    name = config.name,
                                                    categoryId = config.categoryId,
                                                    date = date,
                                                    value = config.valueFixedForType,
                                                    confirmed = false,
                                                    observations = config.observations,
                                                    tags = config.tags?.ifEmpty { null },
                                                    installment = installment,
                                                    recurrenceConfigId = config.id,
                                                    originBillId = originBill?.id,
                                                    targetBillId = targetBill?.id,
                                                ),
                                            ).flatMap { entry ->
                                                updateBalance(
                                                    entry = entry,
                                                    originBill = originBill,
                                                    targetBill = targetBill,
                                                    origin = origin,
                                                    target = target,
                                                ).thenReturn(entry)
                                            }
                                    }.flatMap { walletEntryActionEventService.sendInsertedWalletEntry(config.userId, it).thenReturn(it) }
                            }
                    }
                }
        }

    private fun updateBalance(newEntryRequest: NewEntryRequest): Mono<Void> =
        updateBalance(
            valueForOrigin = newEntryRequest.valueFixedForType,
            valueForTarget = newEntryRequest.value.abs(),
            origin = newEntryRequest.origin!!,
            target = newEntryRequest.target,
            originBill = newEntryRequest.originBill,
            targetBill = newEntryRequest.targetBill,
        )

    private fun updateBalance(
        entry: WalletEntryEntity,
        originBill: CreditCardBillEntity?,
        targetBill: CreditCardBillEntity?,
        origin: WalletItem,
        target: WalletItem?,
    ): Mono<Void> =
        updateBalance(
            valueForOrigin = entry.type.fixValue(entry.value),
            valueForTarget = entry.value.abs(),
            origin = origin,
            target = target,
            originBill = originBill,
            targetBill = targetBill,
        )

    private fun updateBalance(
        valueForOrigin: BigDecimal,
        valueForTarget: BigDecimal,
        origin: WalletItem,
        target: WalletItem?,
        originBill: CreditCardBillEntity?,
        targetBill: CreditCardBillEntity?,
    ): Mono<Void> {
        val updateOriginBalance =
            origin.let { origin ->
                val valueForOrigin = valueForOrigin

                walletItemService.addBalanceById(origin.id!!, valueForOrigin)
            }

        val updateOriginalBillValue =
            originBill?.let { originBill ->
                creditCardBillService.addValueById(
                    originBill.id!!,
                    valueForOrigin,
                )
            } ?: Mono.empty()

        val updateTargetBalance =
            target?.let { origin ->
                walletItemService.addBalanceById(origin.id!!, valueForTarget)
            } ?: Mono.empty()

        val updateTargetBillValue =
            targetBill?.let { targetBill ->
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

        return if (newEntryRequest.inFuture) {
            recurrenceConfigMono.map { it.value!! }
        } else {
            recurrenceConfigMono
                .flatMap { recurrenceConfig ->
                    val installments = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) newEntryRequest.installments!! else 1

                    if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
                        var date: LocalDate = newEntryRequest.date

                        walletEntryRepository
                            .saveAll(
                                (1..installments).map {
                                    date =
                                        when (newEntryRequest.periodicity!!) {
                                            RecurrenceType.SINGLE -> throw IllegalArgumentException(
                                                "Single recurrence is not allowed here.",
                                            )

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
                if (newEntryRequest.inFuture) {
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
                } else {
                    Mono.just(Wrapper.empty())
                }
            }
        }
}
