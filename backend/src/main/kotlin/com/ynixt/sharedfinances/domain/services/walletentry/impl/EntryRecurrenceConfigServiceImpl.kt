package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
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
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.impl.EntityServiceImpl
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Map.entry
import java.util.UUID
import kotlin.collections.associateBy

@Service
class EntryRecurrenceConfigServiceImpl(
    override val repository: RecurrenceEventRepository,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
    private val walletItemService: WalletItemService,
    private val creditCardBillService: CreditCardBillService,
    private val walletItemMapper: WalletItemMapper,
) : EntityServiceImpl<RecurrenceEventEntity, RecurrenceEventEntity>(),
    EntryRecurrenceConfigService {
    private val defaultSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

    override suspend fun getFutureValuesOfWalletItem(
        walletItemId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal =
        findAllEntryByWalletId(
            minimumEndExecution = fixStartDate(minimumEndExecution),
            maximumNextExecution = maximumNextExecution,
            walletItemId = walletItemId,
            userId = if (groupId == null) userId else null,
            groupId = groupId,
        ).toList().sumOf { it.entries?.find { e -> e.walletItemId == walletItemId }?.value ?: BigDecimal.ZERO }

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
                    findAllEntryByWalletId(
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
                    findAllEntryByGroupId(
                        minimumEndExecution = fixedStartDate,
                        maximumNextExecution = maximumNextExecution,
                        groupId = groupId,
                        sort = defaultSort,
                    )
                }

                userId != null -> {
                    findAllEntryByUserId(
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

        return fixInstallment(
            simulateGeneration(
                configs = configs,
                walletItemsById = walletItems.associateBy { it.id!! },
                userById = users.associateBy { it.id!! },
                categoriesById = categories.associateBy { it.id!! },
                groupById = groups.associateBy { it.id!! },
            ).sortedWith(
                compareByDescending<EventListResponse> { it.date }
                    .thenByDescending { it.id },
            ),
            askedBillDate = billDate,
            askedDate = fixedStartDate,
        )
    }

    private fun fixInstallment(
        entries: List<EventListResponse>,
        askedBillDate: LocalDate?,
        askedDate: LocalDate?,
    ): List<EventListResponse> =
        entries.map {
            var correctedInstallment = it.installment

            if (correctedInstallment != null && it.recurrenceConfig != null) {
                if (askedBillDate != null) {
                    val nextBillDate =
                        it.recurrenceConfig.entries!!
                            .filterIsInstance<RecurrenceEntryEntity>()
                            .find { e -> e.nextBillDate != null }
                            ?.nextBillDate
                    val diff =
                        ChronoUnit.MONTHS.between(
                            YearMonth.from(nextBillDate),
                            YearMonth.from(askedBillDate),
                        )

                    if (diff > 0) {
                        correctedInstallment += diff.toInt()
                    }
                } else {
                    val diff =
                        ChronoUnit.MONTHS.between(
                            YearMonth.from(it.recurrenceConfig.nextExecution),
                            YearMonth.from(askedDate),
                        )

                    if (diff > 0) {
                        correctedInstallment += diff.toInt()
                    }
                }

                it.copy(
                    installment = correctedInstallment,
                )
            } else {
                it
            }
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

    private fun findAllEntryByWalletId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        billDate: LocalDate? = null,
        walletItemId: UUID,
        userId: UUID? = null,
        groupId: UUID? = null,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity> {
        require((userId != null) xor (groupId != null))

        return repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = billDate,
                walletItemId = walletItemId,
                userId = userId,
                groupId = groupId,
                sort = sort,
            ).asFlow()
    }

    private fun entryRecurrenceComparator(sort: Sort): Comparator<RecurrenceEventEntity> {
        require(sort.isSorted)

        val orders = sort.toList()

        return Comparator { x, y ->
            for (o in orders) {
                val cmp = compareByOrder(x, y, o)
                if (cmp != 0) return@Comparator cmp
            }
            0
        }
    }

    private fun compareByOrder(
        a: RecurrenceEventEntity,
        b: RecurrenceEventEntity,
        order: Sort.Order,
    ): Int {
        val (va, vb) =
            when (order.property) {
                "nextExecution" -> a.nextExecution to b.nextExecution
                "id" -> a.id to b.id

                else -> error("Sort property not supported: '${order.property}'")
            }

        val nullsFirst =
            when (order.nullHandling) {
                Sort.NullHandling.NULLS_FIRST -> true
                Sort.NullHandling.NULLS_LAST -> false
                Sort.NullHandling.NATIVE -> false // escolha padrão (ajuste se preferir)
            }

        val base = compareNullable(va as Comparable<Any>?, vb as Comparable<Any>?, nullsFirst)

        return if (order.isAscending) base else -base
    }

    private fun <T : Comparable<T>> compareNullable(
        a: T?,
        b: T?,
        nullsFirst: Boolean,
    ): Int {
        if (a === b) return 0
        if (a == null) return if (nullsFirst) -1 else 1
        if (b == null) return if (nullsFirst) 1 else -1
        return a.compareTo(b)
    }

    private fun findAllEntryByUserId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        userId: UUID,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity> =
        repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = null,
                walletItemId = null,
                userId = userId,
                groupId = null,
                sort = sort,
            ).asFlow()

    private fun findAllEntryByGroupId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        groupId: UUID,
        sort: Sort = Sort.unsorted(),
    ): Flow<RecurrenceEventEntity> =
        repository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                billDate = null,
                walletItemId = null,
                userId = null,
                groupId = groupId,
                sort = sort,
            ).asFlow()

    private suspend fun simulateGeneration(
        configs: List<RecurrenceEventEntity>,
        walletItemsById: Map<UUID, WalletItem>,
        userById: Map<UUID, UserEntity>,
        groupById: Map<UUID, GroupEntity>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
    ): List<EventListResponse> =
        configs.map {
            val entries = it.entries!!
            val originEntry = entries.first()
            val targetEntry = entries.getOrNull(1)

            val originWalletItem = walletItemsById[originEntry.walletItemId]!!
            val targetWalletItem = targetEntry?.walletItemId?.let { targetId -> walletItemsById[targetId]!! }
            val user = it.userId?.let { userId -> userById[userId]!! }
            val group = it.groupId?.let { groupId -> groupById[groupId]!! }
            val category = it.categoryId?.let { categoryId -> categoriesById[categoryId]!! }

            simulateGeneration(
                config = it,
                walletItems = listOfNotNull(originWalletItem, targetWalletItem),
                user = user,
                group = group,
                category = category,
                simulateBillForRecurrence = false,
            )
        }

    override suspend fun simulateGeneration(
        config: RecurrenceEventEntity,
        walletItems: List<WalletItem>,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
    ): EventListResponse {
        val installment =
            if (config.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                config.qtyExecuted + 1
            } else {
                null
            }

        val bills =
            walletItems.map {
                if (simulateBillForRecurrence &&
                    it.type == WalletItemType.CREDIT_CARD &&
                    config.nextExecution != null
                ) {
                    simulateBill(it, config.nextExecution, config.userId)
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
                        billDate = bill?.billDate,
                        billId = bill?.id,
                    )
                },
            category = category,
            user = user,
            group = group,
            tags = emptyList(),
            date = config.nextExecution!!,
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
        walletItemEntity: WalletItemEntity,
        billDate: LocalDate?,
        userId: UUID?,
    ): CreditCardBill? {
        if (billDate == null || userId == null) return null

        val walletItem = walletItemMapper.toModel(walletItemEntity)

        return simulateBill(walletItem, billDate, userId)
    }

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
