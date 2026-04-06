package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.OriginNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.TargetNotFoundException
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

abstract class WalletEntrySaveServiceImpl(
    protected val groupService: GroupService,
    protected val walletItemService: WalletItemService,
    protected val creditCardBillService: CreditCardBillService,
    protected val recurrenceService: RecurrenceService,
    protected val recurrenceEventRepository: RecurrenceEventRepository,
    protected val recurrenceEntryRepository: RecurrenceEntryRepository,
    protected val clock: Clock,
) {
    protected suspend fun loadRelationships(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): NewEntryRequest =
        newEntryRequest
            .attachGroup(userId)
            .attachOrigin()
            .attachTarget()
            .attachOriginBill()
            .attachTargetBill()

    protected fun checkDataIntegrity(newEntryRequest: NewEntryRequest) {
        requireNotNull(newEntryRequest.origin)

        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            requireNotNull(newEntryRequest.target)
        }

        if (newEntryRequest.origin.type == WalletItemType.CREDIT_CARD) {
            requireNotNull(newEntryRequest.originBill)
        }

        if (newEntryRequest.target?.type == WalletItemType.CREDIT_CARD) {
            requireNotNull(newEntryRequest.targetBill)
        }

        if (newEntryRequest.paymentType == PaymentType.INSTALLMENTS) {
            requireNotNull(newEntryRequest.installments)
            requireNotNull(newEntryRequest.periodicity)
        }

        if (newEntryRequest.paymentType == PaymentType.RECURRING) {
            requireNotNull(newEntryRequest.periodicity)
        }
    }

    protected fun checkAllPermissions(
        userId: UUID,
        newEntryRequest: NewEntryRequest,
    ): Boolean {
        val hasGroupPermission = newEntryRequest.group == null || newEntryRequest.group.permissions.contains(GroupPermissions.NEW_ENTRY)
        val hasOriginPermission: Boolean
        val hasTargetPermission: Boolean
        val hasCategoryPermission: Boolean

        if (newEntryRequest.group == null) {
            hasOriginPermission = newEntryRequest.origin!!.userId == userId
            hasTargetPermission = newEntryRequest.target == null || newEntryRequest.target.userId == userId
            hasCategoryPermission = newEntryRequest.category == null || newEntryRequest.category.userId == userId
        } else {
            hasOriginPermission = newEntryRequest.group.itemsAssociatedIds.contains(newEntryRequest.origin!!.id!!)
            hasTargetPermission =
                newEntryRequest.target == null ||
                newEntryRequest.group.itemsAssociatedIds.contains(newEntryRequest.target.id!!)
            hasCategoryPermission = newEntryRequest.category == null || newEntryRequest.category.groupId == newEntryRequest.group.id
        }

        return hasGroupPermission && hasOriginPermission && hasTargetPermission && hasCategoryPermission
    }

    protected fun requestToEventEntity(
        id: UUID?,
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        recurrenceConfig: RecurrenceEventEntity?,
        installment: Int?,
        date: LocalDate = newEntryRequest.date,
    ): WalletEventEntity =
        WalletEventEntity(
            type = newEntryRequest.type,
            userId = if (newEntryRequest.groupId == null) userId else null,
            groupId = newEntryRequest.groupId,
            name = newEntryRequest.name,
            categoryId = newEntryRequest.categoryId,
            date = date,
            confirmed = newEntryRequest.confirmed,
            observations = newEntryRequest.observations,
            tags = newEntryRequest.tags?.ifEmpty { null },
            installment = installment,
            recurrenceEventId = recurrenceConfig?.id,
            paymentType = newEntryRequest.paymentType,
        ).also {
            it.id = id
        }

    protected fun requestToEntryEntity(
        id: UUID?,
        event: WalletEventEntity,
        newEntryRequest: NewEntryRequest,
    ): List<WalletEntryEntity> {
        val entries =
            mutableListOf(
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value =
                        if (newEntryRequest.type ==
                            WalletEntryType.TRANSFER
                        ) {
                            newEntryRequest.value.unaryMinus()
                        } else {
                            newEntryRequest.valueFixedForType
                        },
                    walletItemId = newEntryRequest.originId,
                    billId = newEntryRequest.originBill?.id,
                ).also {
                    it.id = id
                },
            )

        if (newEntryRequest.type == WalletEntryType.TRANSFER) {
            entries.add(
                WalletEntryEntity(
                    walletEventId = event.id!!,
                    value = newEntryRequest.value,
                    walletItemId = newEntryRequest.targetId!!,
                    billId = newEntryRequest.targetBill?.id,
                ).also {
                    it.id = id
                },
            )
        }

        return entries
    }

    protected suspend fun requestToRecurrenceEntity(
        id: UUID?,
        userId: UUID,
        newEntryRequest: NewEntryRequest,
        qtyLimit: Int?,
    ): RecurrenceEventEntity {
        val periodicity = newEntryRequest.periodicity ?: RecurrenceType.SINGLE

        val now = LocalDate.now(clock)
        val alreadyExecuted = !newEntryRequest.isInFuture(now)
        val qtyExecuted = if (alreadyExecuted) 1 else 0

        val lastExecution: LocalDate? = if (alreadyExecuted) now else null

        val requestOriginBillDate = newEntryRequest.originBill?.billDate
        val requestTargetBillDate = newEntryRequest.targetBill?.billDate

        val nextOriginBillDate =
            if (requestOriginBillDate == null) {
                null
            } else if (alreadyExecuted) {
                recurrenceService.calculateNextExecution(requestOriginBillDate, periodicity, qtyExecuted, qtyLimit)
            } else {
                requestOriginBillDate
            }

        val nextTargetBillDate =
            if (requestTargetBillDate == null) {
                null
            } else if (alreadyExecuted) {
                recurrenceService.calculateNextExecution(requestTargetBillDate, periodicity, qtyExecuted, qtyLimit)
            } else {
                requestTargetBillDate
            }

        return recurrenceEventRepository
            .save(
                RecurrenceEventEntity(
                    name = newEntryRequest.name,
                    categoryId = newEntryRequest.categoryId,
                    userId = userId,
                    groupId = newEntryRequest.groupId,
                    tags = newEntryRequest.tags,
                    observations = newEntryRequest.observations,
                    type = newEntryRequest.type,
                    periodicity = periodicity,
                    paymentType = newEntryRequest.paymentType,
                    qtyExecuted = qtyExecuted,
                    qtyLimit = qtyLimit,
                    lastExecution = lastExecution,
                    nextExecution =
                        if (alreadyExecuted) {
                            recurrenceService.calculateNextExecution(
                                lastExecution = lastExecution!!,
                                periodicity = periodicity,
                                qtyExecuted = qtyExecuted,
                                qtyLimit = qtyLimit,
                            )
                        } else {
                            newEntryRequest.date
                        },
                    endExecution =
                        recurrenceService.calculateEndDate(
                            lastExecution = lastExecution ?: newEntryRequest.date,
                            periodicity = periodicity,
                            qtyExecuted = qtyExecuted,
                            qtyLimit = qtyLimit?.let { if (lastExecution == null) it - 1 else it },
                        ),
                ).also {
                    it.id = id
                },
            ).awaitSingle()
            .also { savedRecurrence ->
                val entries = mutableListOf<RecurrenceEntryEntity>()

                entries.add(
                    RecurrenceEntryEntity(
                        value =
                            if (newEntryRequest.type ==
                                WalletEntryType.TRANSFER
                            ) {
                                newEntryRequest.value.unaryMinus()
                            } else {
                                newEntryRequest.valueFixedForType
                            },
                        walletEventId = savedRecurrence.id!!,
                        walletItemId = newEntryRequest.originId,
                        nextBillDate = nextOriginBillDate,
                        lastBillDate =
                            if (requestOriginBillDate == null) {
                                null
                            } else {
                                recurrenceService
                                    .calculateEndDate(
                                        lastExecution = requestOriginBillDate,
                                        periodicity = periodicity,
                                        qtyExecuted = qtyExecuted,
                                        qtyLimit = qtyLimit?.let { if (alreadyExecuted) it else it - 1 },
                                    )?.withStartOfMonth()
                            },
                    ),
                )

                if (newEntryRequest.type == WalletEntryType.TRANSFER) {
                    entries.add(
                        RecurrenceEntryEntity(
                            value = newEntryRequest.value,
                            walletEventId = savedRecurrence.id!!,
                            walletItemId = newEntryRequest.targetId!!,
                            nextBillDate = nextTargetBillDate,
                            lastBillDate =
                                if (nextTargetBillDate == null) {
                                    null
                                } else {
                                    recurrenceService
                                        .calculateEndDate(
                                            lastExecution = nextTargetBillDate,
                                            periodicity = periodicity,
                                            qtyExecuted = qtyExecuted,
                                            qtyLimit = qtyLimit?.let { if (alreadyExecuted) it else it - 1 },
                                        )?.withStartOfMonth()
                                },
                        ),
                    )
                }

                savedRecurrence.entries =
                    recurrenceEntryRepository.saveAll(entries).asFlow().toList().also { entries ->
                        entries.forEach { entry ->
                            entry.event = savedRecurrence
                        }
                    }
            }
    }

    private suspend fun <ID, ENTITY : Any> NewEntryRequest.attachRequired(
        id: ID,
        fetch: suspend (ID) -> ENTITY?,
        onFound: NewEntryRequest.(ENTITY) -> NewEntryRequest,
        onNotFound: (ID) -> Throwable,
    ): NewEntryRequest =
        fetch(id)
            ?.let { entity -> onFound(entity) } ?: throw onNotFound(id)

    private suspend fun <ID, ENTITY : Any> NewEntryRequest.attachOptional(
        id: ID?,
        fetch: suspend (ID) -> ENTITY?,
        onFound: NewEntryRequest.(ENTITY) -> NewEntryRequest,
        onNotFound: (ID) -> Throwable,
    ): NewEntryRequest =
        if (id != null) {
            fetch(id)
                ?.let { entity -> onFound(entity) } ?: throw onNotFound(id)
        } else {
            this
        }

    private suspend fun NewEntryRequest.attachGroup(userId: UUID): NewEntryRequest =
        attachOptional(
            id = groupId,
            fetch = { groupId -> groupService.findGroupWithAssociatedItems(userId, groupId) },
            onFound = { group -> copy(group = group) },
            onNotFound = { groupId -> GroupNotFoundException(groupId) },
        )

    private suspend fun NewEntryRequest.attachOrigin(): NewEntryRequest =
        attachRequired(
            id = originId,
            fetch = { originId -> walletItemService.findOne(originId) },
            onFound = { origin -> copy(origin = origin) },
            onNotFound = { originId -> OriginNotFoundException(originId) },
        )

    private suspend fun NewEntryRequest.attachTarget(): NewEntryRequest =
        attachOptional(
            id = targetId,
            fetch = { targetId -> walletItemService.findOne(targetId) },
            onFound = { target -> copy(target = target) },
            onNotFound = { targetId -> TargetNotFoundException(targetId) },
        )

    private suspend fun NewEntryRequest.attachOriginBill(): NewEntryRequest =
        attachBillIfNeeded(
            creditCard = origin as? CreditCard,
            billDate = originBillDate,
        ) { bill ->
            copy(originBill = bill)
        }

    private suspend fun NewEntryRequest.attachTargetBill(): NewEntryRequest =
        attachBillIfNeeded(
            creditCard = target as? CreditCard,
            billDate = targetBillDate,
        ) { bill ->
            copy(targetBill = bill)
        }

    private suspend fun NewEntryRequest.attachBillIfNeeded(
        creditCard: CreditCard?,
        billDate: LocalDate?,
        setBill: NewEntryRequest.(CreditCardBillEntity) -> NewEntryRequest,
    ): NewEntryRequest =
        attachOptional(
            id = if (creditCard != null && billDate != null) creditCard to billDate else null,
            fetch = { (card, billDate) ->
                val dueDate = card.getDueDate(billDate)
                creditCardBillService.getOrCreateBill(
                    creditCardId = requireNotNull(card.id),
                    dueDate = dueDate,
                    closingDate = card.getClosingDate(dueDate),
                )
            },
            onFound = { bill -> setBill(bill) },
            onNotFound = { _ -> IllegalStateException("Unexpected empty bill creation result") },
        )
}
