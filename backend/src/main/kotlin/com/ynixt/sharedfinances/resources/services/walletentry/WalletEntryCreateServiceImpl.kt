package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.isSameMonthYear
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.ifEmpty

@Service
class WalletEntryCreateServiceImpl(
    private val walletEventRepository: WalletEventRepository,
    private val walletEntryRepository: WalletEntryRepository,
    groupService: GroupService,
    walletItemService: WalletItemService,
    creditCardBillService: CreditCardBillService,
    recurrenceService: RecurrenceService,
    recurrenceEventRepository: RecurrenceEventRepository,
    recurrenceEntryRepository: RecurrenceEntryRepository,
    private val walletEventActionEventService: WalletEventActionEventService,
    private val walletItemMapper: WalletItemMapper,
) : WalletEntrySaveServiceImpl(
        groupService = groupService,
        walletItemService = walletItemService,
        creditCardBillService = creditCardBillService,
        recurrenceService = recurrenceService,
        recurrenceEventRepository = recurrenceEventRepository,
        recurrenceEntryRepository = recurrenceEntryRepository,
    ),
    WalletEntryCreateService {
    @Transactional
    override suspend fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEventEntity? =
        loadRelationships(userId, newEntryRequest).let { newEntryRequest ->
            checkDataIntegrity(newEntryRequest)
            if (checkAllPermissions(userId, newEntryRequest)) {
                createWithoutCheckPermissions(userId, newEntryRequest)
                    .also { walletEventActionEventService.sendInsertedWalletEvent(userId, it) }
            } else {
                null
            }
        }

    @Transactional
    override suspend fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): MinimumWalletEventEntity? {
        val event = recurrenceEventRepository.findById(recurrenceConfigId).awaitSingle()

        val entries =
            recurrenceEntryRepository.findAllByWalletEventId(recurrenceConfigId).asFlow().toList().also {
                event.entries =
                    it.also {
                        it.forEach { entry ->
                            entry.event = event
                        }
                    }
            }

        require(event.entries!!.isNotEmpty())

        val nextExecution =
            recurrenceService
                .calculateNextExecution(
                    lastExecution = date,
                    periodicity = event.periodicity,
                    qtyExecuted = event.qtyExecuted + 1,
                    qtyLimit = event.qtyLimit,
                )

        val linesModified =
            recurrenceEventRepository
                .updateConfigCausedByExecution(
                    id = recurrenceConfigId,
                    oldNextExecution = date,
                    nextExecution = nextExecution,
                ).awaitSingle()

        if (linesModified == 0 || event.userId == null) {
            return null
        }

        entries
            .map { entryTemplate ->
                recurrenceEntryRepository.updateNextBillDate(
                    id = entryTemplate.id!!,
                    nextBillDate =
                        if (nextExecution == null) {
                            null
                        } else {
                            calculateNextBillDate(entryTemplate.nextBillDate!!, event.periodicity)
                        },
                )
            }.also {
                Flux.merge(it).then().awaitSingleOrNull()
            }

        val installment =
            if (event.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                event.qtyExecuted + 1
            } else {
                null
            }

        val walletEventSaved =
            walletEventRepository
                .save(
                    WalletEventEntity(
                        type = event.type,
                        userId = event.userId,
                        groupId = event.groupId,
                        name = event.name,
                        categoryId = event.categoryId,
                        date = date,
                        confirmed = false,
                        observations = event.observations,
                        tags = event.tags?.ifEmpty { null },
                        installment = installment,
                        recurrenceEventId = event.id,
                        paymentType = event.paymentType,
                    ),
                ).awaitSingle()

        val walletItems =
            walletItemService
                .findAllByIdIn(
                    entries
                        .map { entry ->
                            entry.walletItemId
                        }.distinct(),
                ).toList()

        val bills =
            entries.map { entry ->
                val walletItem = walletItems.find { it.id!! == entry.walletItemId }!!

                if (walletItem is CreditCard) {
                    val dueDate = walletItem.getDueDate(entry.nextBillDate!!)
                    creditCardBillService
                        .getOrCreateBill(
                            creditCardId = walletItem.id!!,
                            dueDate = dueDate,
                            closingDate = walletItem.getClosingDate(dueDate),
                        )
                } else {
                    null
                }
            }

        walletEventSaved.entries =
            walletEntryRepository
                .saveAll(
                    entries.mapIndexed { index, entryTemplate ->
                        WalletEntryEntity(
                            walletEventId = walletEventSaved.id!!,
                            value = entryTemplate.value,
                            walletItemId = walletItems[index].id!!,
                            billId = bills[index]?.id,
                        )
                    },
                ).asFlow()
                .toList()
                .also { entries ->
                    entries.forEachIndexed { index, saved ->
                        saved.walletItem = walletItemMapper.fromModel(walletItems[index])
                        saved.bill = bills[index]
                        saved.event = walletEventSaved

                        updateBalance(saved)
                    }
                }

        walletEventActionEventService.sendInsertedWalletEvent(event.userId, walletEventSaved)

        return walletEventSaved
    }

    private fun calculateNextBillDate(
        billDate: LocalDate,
        periodicity: RecurrenceType,
    ): LocalDate {
        val candidate =
            this@WalletEntryCreateServiceImpl.recurrenceService.calculateNextDate(
                lastExecution = billDate,
                periodicity = periodicity,
            )

        return if (candidate.isSameMonthYear(billDate)) billDate else candidate.withDayOfMonth(1)
    }

    private suspend fun updateBalance(entry: WalletEntryEntity) {
        val event = entry.event!! as WalletEventEntity

        if (event.paymentType == PaymentType.INSTALLMENTS && entry.walletItem!!.type == WalletItemType.CREDIT_CARD) {
            if (event.installment == 1) {
                val installments = event.recurrenceEvent!!.qtyLimit!!

                walletItemService.addBalanceById(entry.walletItemId, entry.value.multiply(installments.toBigDecimal()))
            }
        } else {
            walletItemService.addBalanceById(entry.walletItemId, entry.value)
        }

        if (entry.billId != null) {
            creditCardBillService.addValueById(
                entry.billId,
                entry.value,
            )
        }
    }

    private suspend fun createWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEventEntity {
        val recurrenceConfig = createRecurrenceConfig(userId, newEntryRequest)

        return if (newEntryRequest.inFuture) {
            recurrenceConfig!!
        } else {
            createNowWithoutCheckPermissions(
                userId = userId,
                newEntryRequest = newEntryRequest,
                recurrenceEvent = recurrenceConfig,
            )
        }.also {
            it.entries!!.forEachIndexed { index, entity ->
                entity.walletItem =
                    (
                        if (index ==
                            0
                        ) {
                            newEntryRequest.origin
                        } else {
                            newEntryRequest.target
                        }
                    )?.let { origin -> walletItemMapper.fromModel(origin) }
            }
        }
    }

    private suspend fun createNowWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        recurrenceEvent: RecurrenceEventEntity?,
    ): MinimumWalletEventEntity {
        val event =
            walletEventRepository
                .save(
                    requestToEventEntity(
                        id = null,
                        userId = userId,
                        newEntryRequest = newEntryRequest,
                        recurrenceConfig = recurrenceEvent,
                        installment = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) 1 else null,
                        date = newEntryRequest.date,
                    ),
                ).awaitSingle()

        event.recurrenceEvent = recurrenceEvent

        event.entries =
            walletEntryRepository
                .saveAll(
                    requestToEntryEntity(
                        id = null,
                        event = event,
                        newEntryRequest = newEntryRequest,
                    ),
                ).asFlow()
                .toList()
                .also {
                    it.forEach { entry ->
                        entry.event = event
                    }
                }

        return event.also {
            it.entries!!.filterIsInstance<WalletEntryEntity>().forEachIndexed { index, entry ->
                entry.bill = if (index == 0) newEntryRequest.originBill else newEntryRequest.targetBill
                entry.walletItem = walletItemMapper.fromModel(if (index == 0) newEntryRequest.origin!! else newEntryRequest.target!!)

                updateBalance(entry)
            }
        }
    }

    private suspend fun createRecurrenceConfig(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): RecurrenceEventEntity? =
        when (newEntryRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                requestToRecurrenceEntity(
                    id = null,
                    userId = userId,
                    newEntryRequest = newEntryRequest,
                    qtyLimit = newEntryRequest.installments!!,
                )

            PaymentType.RECURRING ->
                requestToRecurrenceEntity(
                    id = null,
                    userId = userId,
                    newEntryRequest = newEntryRequest,
                    qtyLimit = newEntryRequest.periodicityQtyLimit,
                )

            else -> {
                if (newEntryRequest.inFuture) {
                    requestToRecurrenceEntity(
                        id = null,
                        userId = userId,
                        newEntryRequest = newEntryRequest,
                        qtyLimit = 1,
                    )
                } else {
                    null
                }
            }
        }
}
