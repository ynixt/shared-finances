package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.enums.EntrySummaryType
import com.ynixt.sharedfinances.domain.models.SummaryEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGrouped
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySummaryGroupedResult
import com.ynixt.sharedfinances.domain.models.walletentry.plus
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntrySummaryService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Service
class WalletEntrySummaryServiceImpl(
    private val walletEntryRepository: WalletEntryRepository,
    private val walletItemService: WalletItemService,
    private val userService: UserService,
    private val groupPermissionService: GroupPermissionService,
) : WalletEntrySummaryService {
    override fun summary(
        userId: UUID,
        groupId: UUID?,
        request: SummaryEntryRequest,
    ): Mono<EntrySummary> {
        val permissionMono =
            if (groupId != null) groupPermissionService.hasPermission(userId = userId, groupId = groupId) else Mono.just(true)

        return permissionMono.flatMap { hasPermission ->
            if (hasPermission) {
                when (request.summaryType) {
                    EntrySummaryType.BANK_ACCOUNT ->
                        bankAccountSummary(
                            userId = if (groupId == null) userId else null,
                            groupId = groupId,
                            request = request,
                        )

                    else -> TODO()
                }
            } else {
                Mono.empty()
            }
        }
    }

    // TODO creditCard -> need to get summary looking to closingDate instead of date
    // TODO dash -> need to get bankAccount summary and then sum on "projected" all creditCards

    private fun bankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        request: SummaryEntryRequest,
    ) = walletEntryRepository
        .sumForBankAccountSummary(
            userId = userId,
            groupId = groupId,
            walletItemId = request.walletItemId,
            minimumDate = request.minimumDate ?: LocalDate.now().minusMonths(1),
            maximumDate = request.maximumDate,
        ).collectList()
        .defaultIfEmpty(emptyList())
        .flatMap { sumsNotGrouped ->
            var sums =
                sumsNotGrouped
                    .groupBy { it.walletItemId }
                    .mapValues { it.value.reduce { acc, entry -> acc + entry } }
                    .values
                    .toList()

            val walletItemsIds = sums.map { it.walletItemId }.toSet()

            walletItemService.findAllByIdIn(walletItemsIds).collectList().flatMap { walletItems ->
                userService.findAllByIdIn(walletItems.mapNotNull { it.userId }).collectList().defaultIfEmpty(emptyList()).map { users ->
                    val walletItemById = walletItems.associateBy { it.id!! }
                    val userById = users.associateBy { it.id!! }

                    walletItemById.values.forEach { it.user = userById[it.userId] }
                    sums.forEach { it.walletItem = walletItemById[it.walletItemId] }

                    if (groupId == null) {
                        sums = sums.filter { it.walletItem!!.userId == userId }
                    }

                    val sumsGroupedByWalletItem = sums.groupBy { it.walletItem!! }

                    var total = EntrySum.EMPTY
                    var totalProjected = EntrySum.EMPTY
                    var totalPeriod = EntrySum.EMPTY

                    sums.forEach {
                        total += it.sum
                        totalProjected += it.projected
                        totalPeriod += it.period
                    }

                    EntrySummary(
                        total = total,
                        totalProjected = totalProjected,
                        totalPeriod = totalPeriod,
                        grouped =
                            sumsGroupedByWalletItem.map {
                                EntrySummaryGrouped(
                                    walletItem = it.key,
                                    entries =
                                        it.value.map { sumResult ->
                                            EntrySummaryGroupedResult(
                                                sum = sumResult.sum,
                                                projected = sumResult.projected,
                                                period = sumResult.period,
                                            )
                                        },
                                )
                            },
                    )
                }
            }
        }
}
