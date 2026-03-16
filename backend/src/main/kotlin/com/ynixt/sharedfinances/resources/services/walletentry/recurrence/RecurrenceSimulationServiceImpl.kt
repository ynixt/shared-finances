package com.ynixt.sharedfinances.resources.services.walletentry.recurrence

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.extensions.LocalDateExtensions.withStartOfMonth
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.plus
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceOccurrenceSimulationService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class RecurrenceSimulationServiceImpl(
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
    private val walletItemService: WalletItemService,
    private val creditCardBillService: CreditCardBillService,
    private val walletItemMapper: WalletItemMapper,
    private val recurrenceService: RecurrenceService,
    private val recurrenceOccurrenceSimulationService: RecurrenceOccurrenceSimulationService,
) : RecurrenceSimulationService {
    private val defaultSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

    override suspend fun getFutureValuesOfWalletItem(
        walletItemId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal =
        recurrenceService
            .findAllEntryByWalletId(
                minimumEndExecution = fixStartDate(minimumEndExecution),
                maximumNextExecution = maximumNextExecution,
                walletItemId = walletItemId,
                userId = if (groupId == null) userId else null,
                groupId = groupId,
            ).toList()
            .sumOf { it.entries?.find { e -> e.walletItemId == walletItemId }?.value ?: BigDecimal.ZERO }

    override suspend fun getFutureValuesOCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): BigDecimal =
        simulateGenerationForCreditCard(
            bill = bill,
            walletItemId = walletItemId,
            userId = userId,
            groupId = groupId,
        ).sumOf { it.entries.filter { entry -> entry.walletItemId == walletItemId }.sumOf { entry -> entry.value } }

    override suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): List<EventListResponse> {
        val bill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItemId,
                billDate = billDate,
            )

        return simulateGenerationForCreditCard(
            bill = bill,
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
        )
    }

    override suspend fun simulateGenerationForCreditCard(
        bill: CreditCardBill,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID?,
    ): List<EventListResponse> =
        simulateGeneration(
            minimumEndExecution = null,
            maximumNextExecution = null,
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
            billDate = bill.billDate,
        )

    override suspend fun simulateGeneration(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        billDate: LocalDate?,
    ): List<EventListResponse> {
        val fixedStartDate = fixStartDate(minimumEndExecution)

        val configs =
            when {
                walletItemId != null -> {
                    recurrenceService.findAllEntryByWalletId(
                        minimumEndExecution = fixedStartDate,
                        maximumNextExecution = maximumNextExecution,
                        walletItemId = walletItemId,
                        userId = if (groupId == null) userId else null,
                        groupId = groupId,
                        billDate = billDate,
                        sort = defaultSort,
                    )
                }

                groupId != null -> {
                    recurrenceService.findAllEntryByGroupId(
                        minimumEndExecution = fixedStartDate,
                        maximumNextExecution = maximumNextExecution,
                        groupId = groupId,
                        sort = defaultSort,
                    )
                }

                userId != null -> {
                    recurrenceService.findAllEntryByUserId(
                        minimumEndExecution = fixedStartDate,
                        maximumNextExecution = maximumNextExecution,
                        userId = userId,
                        sort = defaultSort,
                    )
                }

                else -> TODO()
            }.toList()

        val categories = genericCategoryService.findAllByIdIn(configs.mapNotNull { config -> config.categoryId }.toSet()).toList()
        val groups = groupService.findAllByIdIn(configs.mapNotNull { config -> config.categoryId }.toSet()).toList()
        val users = userService.findAllByIdIn(configs.mapNotNull { config -> config.userId }.toSet()).toList()
        val walletItems = walletItemService.findAllByIdIn(configs.flatMap { it.entries!! }.map { it.walletItemId }.toSet()).toList()

        return simulateGeneration(
            configs = configs,
            walletItemsById = walletItems.associateBy { it.id!! },
            userById = users.associateBy { it.id!! },
            categoriesById = categories.associateBy { it.id!! },
            groupById = groups.associateBy { it.id!! },
            minimumDate = fixedStartDate,
            maximumDate = maximumNextExecution,
            askedBillDate = billDate,
            askedWalletItemId = walletItemId,
            requestUserId = userId,
        ).sortedWith(
            compareByDescending<EventListResponse> { it.date }
                .thenByDescending { it.id },
        )
    }

    override suspend fun simulateGenerationAsEntrySumResult(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
    ): List<EntrySumResult> {
        val resultByWalletId = mutableMapOf<UUID, EntrySumResult>()

        simulateGeneration(
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            billDate = null,
        ).forEach {
            it.entries.forEach { entry ->
                if (!resultByWalletId.containsKey(entry.walletItemId)) {
                    resultByWalletId[entry.walletItemId] = EntrySumResult.empty(entry.walletItemId)
                }

                val projected =
                    EntrySum(
                        balance = entry.value,
                        revenue = if (entry.value >= BigDecimal.ZERO) entry.value else BigDecimal.ZERO,
                        expense = if (entry.value < BigDecimal.ZERO) entry.value.negate() else BigDecimal.ZERO,
                    )

                resultByWalletId[entry.walletItemId] = resultByWalletId[entry.walletItemId]!! +
                    EntrySumResult(
                        sum = EntrySum.EMPTY,
                        period = EntrySum.EMPTY,
                        projected = projected,
                        walletItemId = entry.walletItemId,
                        creditCardBillId = null,
                    ).also { result ->
                        result.walletItem = entry.walletItem
                        result.user = it.user
                    }
            }
        }

        return resultByWalletId.values.toList()
    }

    private suspend fun simulateGeneration(
        configs: List<RecurrenceEventEntity>,
        walletItemsById: Map<UUID, WalletItem>,
        userById: Map<UUID, UserEntity>,
        groupById: Map<UUID, GroupEntity>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        askedBillDate: LocalDate?,
        askedWalletItemId: UUID?,
        requestUserId: UUID?,
    ): List<EventListResponse> {
        val simulatedEntries = mutableListOf<EventListResponse>()

        configs.forEach {
            val entries = it.entries!!
            val originEntry = entries.first()
            val targetEntry = entries.getOrNull(1)

            val originWalletItem = walletItemsById[originEntry.walletItemId]!!
            val targetWalletItem = targetEntry?.walletItemId?.let { targetId -> walletItemsById[targetId]!! }
            val user = it.userId?.let { userId -> userById[userId]!! }
            val group = it.groupId?.let { groupId -> groupById[groupId]!! }
            val category = it.categoryId?.let { categoryId -> categoriesById[categoryId]!! }
            val walletItems = listOfNotNull(originWalletItem, targetWalletItem)
            val occurrences =
                recurrenceOccurrenceSimulationService.buildOccurrences(
                    config = it,
                    walletItems = walletItems,
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                    askedBillDate = askedBillDate,
                    askedWalletItemId = askedWalletItemId,
                    requestUserId = requestUserId,
                )

            occurrences.forEach { occurrence ->
                simulatedEntries.add(
                    simulateGeneration(
                        config = it,
                        walletItems = walletItems,
                        user = user,
                        group = group,
                        category = category,
                        simulateBillForRecurrence = false,
                        simulatedDate = occurrence.executionDate,
                        simulatedInstallment = occurrence.installment,
                        simulatedBillDateByWalletItemId = occurrence.billDateByWalletItemId,
                    ),
                )
            }
        }
        return simulatedEntries
    }

    override suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse =
        simulateGeneration(
            config = config,
            walletItems = walletItems,
            user = user,
            group = group,
            category = category,
            simulateBillForRecurrence = simulateBillForRecurrence,
            simulatedDate = null,
            simulatedInstallment = null,
            simulatedBillDateByWalletItemId = null,
        )

    private suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
        simulatedDate: LocalDate?,
        simulatedInstallment: Int?,
        simulatedBillDateByWalletItemId: Map<UUID, LocalDate?>?,
    ): EventListResponse {
        val installment =
            if (simulatedInstallment != null) {
                simulatedInstallment
            } else if (config.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                config.qtyExecuted + 1
            } else {
                null
            }

        val executionDate = simulatedDate ?: config.nextExecution!!

        val bills =
            walletItems.map {
                if (simulatedBillDateByWalletItemId != null) {
                    null
                } else if (simulateBillForRecurrence &&
                    it.type == WalletItemType.CREDIT_CARD
                ) {
                    simulateBill(it, executionDate, config.userId)
                } else {
                    null
                }
            }

        return EventListResponse(
            id = null,
            type = config.type,
            name = config.name,
            entries =
                config.entries!!.mapIndexed { index, entry ->
                    val bill = bills[index]

                    EventListResponse.EntryResponse(
                        value = entry.value,
                        walletItemId = entry.walletItemId,
                        walletItem = walletItems.find { wt -> wt.id == entry.walletItemId }!!,
                        billDate = simulatedBillDateByWalletItemId?.get(entry.walletItemId) ?: bill?.billDate,
                        billId = bill?.id,
                    )
                },
            category = category,
            user = user,
            group = group,
            tags = emptyList(),
            date = executionDate,
            observations = config.observations,
            recurrenceConfigId = config.id!!,
            recurrenceConfig = config,
            confirmed = false,
            currency = walletItems.first().currency,
            installment = installment,
        )
    }

    private fun fixStartDate(startDate: LocalDate?): LocalDate? =
        if (startDate == null || startDate.isBefore(LocalDate.now().plusDays(1))) LocalDate.now().plusDays(1) else startDate

    private suspend fun simulateBill(
        walletItem: WalletItem,
        billDate: LocalDate?,
        userId: UUID?,
    ): CreditCardBill? {
        if (billDate == null || userId == null) return null
        return if (walletItem is CreditCard) {
            val dueDate = walletItem.getDueDate(billDate)

            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItem.id!!,
                billDate = dueDate.withStartOfMonth(),
            )
        } else {
            null
        }
    }
}
