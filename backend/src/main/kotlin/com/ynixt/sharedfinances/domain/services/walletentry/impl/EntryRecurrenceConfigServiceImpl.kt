package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.walletentry.EntryListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.models.walletentry.plus
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import com.ynixt.sharedfinances.domain.util.FlowMergeSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.associateBy

@Service
class EntryRecurrenceConfigServiceImpl(
    private val entryRecurrenceConfigRepository: EntryRecurrenceConfigRepository,
    private val genericCategoryService: GenericCategoryService,
    private val groupService: GroupService,
    private val userService: UserService,
    private val walletItemService: WalletItemService,
    private val creditCardBillService: CreditCardBillService,
    private val walletItemMapper: WalletItemMapper,
) : EntryRecurrenceConfigService {
    private val defaultSort = Sort.by(Sort.Direction.DESC, "nextExecution", "id")

    override suspend fun getFutureValuesOfWalletItem(
        walletId: UUID,
        minimumEndExecution: LocalDate,
        maximumNextExecution: LocalDate,
        userId: UUID,
        groupId: UUID?,
    ): BigDecimal =
        findAllEntryByWalletId(
            minimumEndExecution = fixStartDate(minimumEndExecution),
            maximumNextExecution = maximumNextExecution,
            walletId = walletId,
            userId = if (groupId == null) userId else null,
            groupId = groupId,
        ).toList().sumOf { if (it.originId == walletId) it.value else it.value.negate() }

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
        ).sumOf { if (it.origin.id == walletItemId) it.value else it.value.negate() }

    override suspend fun simulateGenerationForCreditCard(
        billDate: LocalDate,
        userId: UUID,
        groupId: UUID?,
        walletItemId: UUID,
    ): List<EntryListResponse> {
        val bill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItemId,
                billDate = billDate,
            )

        val previousBill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = walletItemId,
                billDate = billDate.minusMonths(1),
            )

        bill.startDate = previousBill.closingDate

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
    ): List<EntryListResponse> {
        require(bill.startDate != null)

        return simulateGeneration(
            minimumEndExecution = bill.startDate,
            maximumNextExecution = bill.closingDate.minusDays(1),
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
        )
    }

    override suspend fun simulateGeneration(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
    ): List<EntryListResponse> {
        val fixedStartDate = fixStartDate(minimumEndExecution)

        val configs =
            when {
                walletItemId != null -> {
                    findAllEntryByWalletId(
                        minimumEndExecution = fixedStartDate,
                        maximumNextExecution = maximumNextExecution,
                        walletId = walletItemId,
                        userId = if (groupId == null) userId else null,
                        groupId = groupId,
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
        val walletItems =
            walletItemService
                .findAllByIdIn(
                    (
                        configs.map { config -> config.originId } + configs.mapNotNull { config -> config.targetId }
                    ).toSet(),
                ).toList()

        return simulateGeneration(
            configs = configs,
            walletItemsById = walletItems.associateBy { it.id!! },
            userById = users.associateBy { it.id!! },
            categoriesById = categories.associateBy { it.id!! },
            groupById = groups.associateBy { it.id!! },
        ).sortedWith(
            compareByDescending<EntryListResponse> { it.date }
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
        ).forEach {
            if (!resultByWalletId.containsKey(it.origin.id)) {
                resultByWalletId[it.origin.id!!] = EntrySumResult.empty(it.origin.id!!)
            }

            val projected =
                EntrySum(
                    balance = it.value,
                    revenue = if (it.value >= BigDecimal.ZERO) it.value else BigDecimal.ZERO,
                    expense = if (it.value < BigDecimal.ZERO) it.value.negate() else BigDecimal.ZERO,
                )

            it.origin.let { origin ->
                resultByWalletId[origin.id!!] = resultByWalletId[origin.id!!]!! +
                    EntrySumResult(
                        sum = EntrySum.EMPTY,
                        period = EntrySum.EMPTY,
                        projected = projected,
                        walletItemId = origin.id!!,
                        creditCardBillId = null,
                    ).also { result ->
                        result.walletItem = origin
                        result.user = it.user
                    }
            }

            it.target?.let { target ->
                if (!resultByWalletId.containsKey(target.id)) {
                    resultByWalletId[target.id!!] = EntrySumResult.empty(target.id!!)
                }

                val projectedInverse =
                    projected.copy(
                        balance = projected.balance.negate(),
                        revenue = projected.expense,
                        expense = projected.revenue,
                    )

                resultByWalletId[target.id!!] = resultByWalletId[target.id!!]!! +
                    EntrySumResult(
                        sum = projectedInverse,
                        period = EntrySum.EMPTY,
                        projected = projectedInverse,
                        walletItemId = target.id!!,
                        creditCardBillId = null,
                    ).also { result ->
                        result.walletItem = target
                        result.user = it.user
                    }
            }
        }

        return resultByWalletId.values.toList()
    }

    private fun findAllEntryByWalletId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        walletId: UUID,
        userId: UUID? = null,
        groupId: UUID? = null,
        sort: Sort = Sort.unsorted(),
    ): Flow<EntryRecurrenceConfigEntity> {
        require((userId != null) xor (groupId != null))

        val originFlow =
            entryRecurrenceConfigRepository
                .findAll(
                    minimumEndExecution = minimumEndExecution,
                    maximumNextExecution = maximumNextExecution,
                    originId = walletId,
                    targetId = null,
                    userId = userId,
                    groupId = groupId,
                    sort = sort,
                ).asFlow()

        val targetFlow =
            entryRecurrenceConfigRepository
                .findAll(
                    minimumEndExecution = minimumEndExecution,
                    maximumNextExecution = maximumNextExecution,
                    originId = null,
                    targetId = walletId,
                    userId = userId,
                    groupId = groupId,
                    sort = sort,
                ).asFlow()

        return if (sort.isUnsorted) {
            merge(originFlow, targetFlow)
        } else {
            val comparator = entryRecurrenceComparator(sort)
            FlowMergeSort.mergeSorted(originFlow, targetFlow, comparator)
        }
    }

    private fun entryRecurrenceComparator(sort: Sort): Comparator<EntryRecurrenceConfigEntity> {
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
        a: EntryRecurrenceConfigEntity,
        b: EntryRecurrenceConfigEntity,
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
    ): Flow<EntryRecurrenceConfigEntity> =
        entryRecurrenceConfigRepository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                originId = null,
                targetId = null,
                userId = userId,
                groupId = null,
                sort = sort,
            ).asFlow()

    private fun findAllEntryByGroupId(
        minimumEndExecution: LocalDate? = null,
        maximumNextExecution: LocalDate? = null,
        groupId: UUID,
        sort: Sort = Sort.unsorted(),
    ): Flow<EntryRecurrenceConfigEntity> =
        entryRecurrenceConfigRepository
            .findAll(
                minimumEndExecution = minimumEndExecution,
                maximumNextExecution = maximumNextExecution,
                originId = null,
                targetId = null,
                userId = null,
                groupId = groupId,
                sort = sort,
            ).asFlow()

    private suspend fun simulateGeneration(
        configs: List<EntryRecurrenceConfigEntity>,
        walletItemsById: Map<UUID, WalletItem>,
        userById: Map<UUID, UserEntity>,
        groupById: Map<UUID, GroupEntity>,
        categoriesById: Map<UUID, WalletEntryCategoryEntity>,
    ): List<EntryListResponse> =
        configs.map {
            val origin = walletItemsById[it.originId]!!
            val target = it.targetId?.let { targetId -> walletItemsById[targetId]!! }
            val user = it.userId?.let { userId -> userById[userId]!! }
            val group = it.groupId?.let { groupId -> groupById[groupId]!! }
            val category = it.categoryId?.let { categoryId -> categoriesById[categoryId]!! }

            simulateGeneration(
                config = it,
                origin = origin,
                target = target,
                user = user,
                group = group,
                category = category,
                simulateBillForRecurrence = false,
            )
        }

    override suspend fun simulateGeneration(
        config: EntryRecurrenceConfigEntity,
        origin: WalletItem,
        target: WalletItem?,
        user: UserEntity?,
        group: GroupEntity?,
        category: WalletEntryCategoryEntity?,
        simulateBillForRecurrence: Boolean,
    ): EntryListResponse {
        val installment =
            if (config.paymentType ==
                PaymentType.INSTALLMENTS
            ) {
                config.qtyExecuted + 1
            } else {
                null
            }

        val originBill =
            if (simulateBillForRecurrence &&
                config.origin?.type == WalletItemType.CREDIT_CARD &&
                config.nextExecution != null
            ) {
                config.origin?.let { origin ->
                    simulateBill(origin, config.nextExecution, config.userId)
                }
            } else {
                null
            }

        val targetBill =
            if (simulateBillForRecurrence &&
                config.target?.type == WalletItemType.CREDIT_CARD &&
                config.nextExecution != null
            ) {
                config.target?.let { target ->
                    simulateBill(target, config.nextExecution, config.userId)
                }
            } else {
                null
            }

        return EntryListResponse(
            id = null,
            type = config.type,
            name = config.name,
            value = config.value,
            category = category,
            user = user,
            group = group,
            tags = emptyList(),
            date = config.nextExecution!!,
            observations = config.observations,
            recurrenceConfigId = config.id!!,
            confirmed = false,
            currency = origin.currency,
            installment = installment,
            origin = origin,
            target = target,
            originBillDate = originBill?.billDate,
            originBillId = originBill?.id,
            targetBillDate = targetBill?.billDate,
            targetBillId = targetBill?.id,
        )
    }

    private fun fixStartDate(startDate: LocalDate?): LocalDate? =
        if (startDate == null || startDate.isBefore(LocalDate.now().plusDays(1))) LocalDate.now().plusDays(1) else startDate

    private suspend fun simulateBill(
        walletItemEntity: WalletItemEntity,
        nextExecution: LocalDate?,
        userId: UUID?,
    ): CreditCardBill? {
        if (nextExecution == null || userId == null) return null

        val creditCard = walletItemMapper.toModel(walletItemEntity)

        return if (creditCard is CreditCard) {
            val billDate = creditCard.getBestBill(nextExecution)
            val dueDate = creditCard.getDueDate(billDate)

            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = creditCard.id!!,
                billDate = dueDate.withDayOfMonth(1),
            )
        } else {
            null
        }
    }
}
