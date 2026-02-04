package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEntry
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEntryActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    override suspend fun create(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEntry? =
        loadRelationships(userId, newEntryRequest).let { newEntryRequest ->
            checkDataIntegrity(newEntryRequest)
            if (checkAllPermissions(userId, newEntryRequest)) {
                createWithoutCheckPermissions(userId, newEntryRequest)
                    .also { walletEntryActionEventService.sendInsertedWalletEntry(userId, it) }
            } else {
                null
            }
        }

    @Transactional
    override suspend fun createFromRecurrenceConfig(
        recurrenceConfigId: UUID,
        date: LocalDate,
    ): MinimumWalletEntry? {
        val config = entryRecurrenceConfigRepository.findById(recurrenceConfigId).awaitSingle()

        val linesModified =
            entryRecurrenceConfigRepository
                .updateConfigCausedByExecution(
                    id = recurrenceConfigId,
                    oldNextExecution = date,
                    nextExecution =
                        entryRecurrenceService.calculateNextExecution(
                            lastExecution = date,
                            periodicity = config.periodicity,
                            qtyExecuted = config.qtyExecuted,
                            qtyLimit = config.qtyLimit,
                        ),
                ).awaitSingle()

        if (linesModified == 0 || config.userId == null) {
            return null
        }

        val executionDate = config.nextExecution!!

        val walletItems =
            walletItemService
                .findAllByIdIn(
                    listOfNotNull(config.originId, config.targetId).distinct(),
                ).toList()

        val origin = walletItems.find { it.id == config.originId }!!
        val target = walletItems.find { it.id == config.targetId }

        val originBill =
            if (origin is CreditCard) {
                val billDate = origin.getBestBill(executionDate)
                val dueDate = origin.getDueDate(billDate)
                creditCardBillService
                    .getOrCreateBill(
                        creditCardId = origin.id!!,
                        dueDate = dueDate,
                        closingDate = origin.getClosingDate(dueDate),
                    )
            } else {
                null
            }

        val targetBill =
            if (target != null && target is CreditCard) {
                val billDate = target.getBestBill(executionDate)
                val dueDate = target.getDueDate(billDate)
                creditCardBillService
                    .getOrCreateBill(
                        creditCardId = target.id!!,
                        dueDate = dueDate,
                        closingDate = target.getClosingDate(dueDate),
                    )
            } else {
                null
            }

        val installment =
            if (config.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                config.qtyExecuted + 1
            } else {
                null
            }

        val walletEntrySaved =
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
                        value = config.value,
                        confirmed = false,
                        observations = config.observations,
                        tags = config.tags?.ifEmpty { null },
                        installment = installment,
                        recurrenceConfigId = config.id,
                        originBillId = originBill?.id,
                        targetBillId = targetBill?.id,
                    ),
                ).awaitSingle()

        updateBalance(
            entry = walletEntrySaved,
            originBill = originBill,
            targetBill = targetBill,
            origin = origin,
            target = target,
        )

        walletEntryActionEventService.sendInsertedWalletEntry(config.userId, walletEntrySaved)

        return walletEntrySaved
    }

    private suspend fun updateBalance(newEntryRequest: NewEntryRequest) =
        updateBalance(
            valueForOrigin = newEntryRequest.valueFixedForType,
            valueForTarget = newEntryRequest.value.abs(),
            origin = newEntryRequest.origin!!,
            target = newEntryRequest.target,
            originBill = newEntryRequest.originBill,
            targetBill = newEntryRequest.targetBill,
        )

    private suspend fun updateBalance(
        entry: WalletEntryEntity,
        originBill: CreditCardBillEntity?,
        targetBill: CreditCardBillEntity?,
        origin: WalletItem,
        target: WalletItem?,
    ) = updateBalance(
        valueForOrigin = entry.type.fixValue(entry.value),
        valueForTarget = entry.value.abs(),
        origin = origin,
        target = target,
        originBill = originBill,
        targetBill = targetBill,
    )

    private suspend fun updateBalance(
        valueForOrigin: BigDecimal,
        valueForTarget: BigDecimal,
        origin: WalletItem,
        target: WalletItem?,
        originBill: CreditCardBillEntity?,
        targetBill: CreditCardBillEntity?,
    ) {
        walletItemService.addBalanceById(origin.id!!, valueForOrigin)

        if (originBill != null) {
            creditCardBillService.addValueById(
                originBill.id!!,
                valueForOrigin,
            )
        }

        if (target != null) {
            walletItemService.addBalanceById(target.id!!, valueForTarget)
        }

        if (targetBill != null) {
            creditCardBillService.addValueById(
                targetBill.id!!,
                valueForTarget,
            )
        }
    }

    private suspend fun createWithoutCheckPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): MinimumWalletEntry {
        val recurrenceConfig = createRecurrenceConfig(userId, newEntryRequest)

        return if (newEntryRequest.inFuture) {
            recurrenceConfig!!
        } else {
            val entry =
                walletEntryRepository
                    .save(
                        requestToEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            recurrenceConfig = recurrenceConfig,
                            installment = if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) 1 else 0,
                            date = newEntryRequest.date,
                        ),
                    ).awaitSingle()

            entry.also { updateBalance(newEntryRequest) }
        }
    }

    private suspend fun createRecurrenceConfig(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): EntryRecurrenceConfigEntity? =
        when (newEntryRequest.paymentType) {
            PaymentType.INSTALLMENTS ->
                entryRecurrenceConfigRepository
                    .save(
                        requestToRecurrenceEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            qtyLimit = newEntryRequest.installments!!,
                        ),
                    ).awaitSingle()

            PaymentType.RECURRING ->
                entryRecurrenceConfigRepository
                    .save(
                        requestToRecurrenceEntity(
                            id = null,
                            userId = userId,
                            newEntryRequest = newEntryRequest,
                            qtyLimit = newEntryRequest.periodicityQtyLimit,
                        ),
                    ).awaitSingle()

            else -> {
                if (newEntryRequest.inFuture) {
                    entryRecurrenceConfigRepository
                        .save(
                            requestToRecurrenceEntity(
                                id = null,
                                userId = userId,
                                newEntryRequest = newEntryRequest,
                                qtyLimit = 1,
                            ),
                        ).awaitSingle()
                } else {
                    null
                }
            }
        }
}
