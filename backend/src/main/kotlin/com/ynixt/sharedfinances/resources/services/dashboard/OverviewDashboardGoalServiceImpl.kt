package com.ynixt.sharedfinances.resources.services.dashboard

import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetail
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboardDetailSourceType
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
internal class OverviewDashboardGoalServiceImpl(
    private val goalLedgerSummaryRepository: GoalLedgerCommittedSummaryRepository,
) {
    internal suspend fun loadCommittedRawDetails(
        userId: UUID,
        bankAccountIds: Set<UUID>,
        referenceDate: LocalDate,
    ): List<RawDetail> =
        goalLedgerSummaryRepository
            .summarizeCommittedByUserGoalsDetailed(userId)
            .collectList()
            .awaitSingle()
            .filter { committed -> bankAccountIds.contains(committed.walletItemId) }
            .map { committed ->
                RawDetail(
                    sourceId = committed.goalId,
                    sourceType = OverviewDashboardDetailSourceType.GOAL,
                    label = committed.goalName,
                    value = committed.committed.asMoney(),
                    currency = committed.currency,
                    referenceDate = referenceDate,
                    walletItemId = committed.walletItemId,
                )
            }

    internal suspend fun hasAccountOverCommittedBalance(
        userId: UUID,
        bankAccountIds: Set<UUID>,
        bankAccountById: Map<UUID, BankAccount>,
        rawBalanceByBankId: Map<UUID, BigDecimal>,
    ): Boolean =
        goalLedgerSummaryRepository
            .summarizeCommittedByUserGoals(userId)
            .collectList()
            .awaitSingle()
            .filter { committed -> bankAccountIds.contains(committed.walletItemId) }
            .any { committed ->
                val bankAccount = bankAccountById[committed.walletItemId] ?: return@any false
                if (!committed.currency.equals(bankAccount.currency, ignoreCase = true)) {
                    return@any false
                }
                val balance = rawBalanceByBankId[committed.walletItemId] ?: BigDecimal.ZERO
                committed.committed.asMoney().compareTo(balance) > 0
            }

    internal fun buildGoalCommittedDetailsByWallet(
        rawGoalCommittedDetails: List<RawDetail>,
        convertedValueByKey: Map<String, BigDecimal>,
        visibleBankAccounts: List<BankAccount>,
        rawBalanceByBankId: Map<UUID, BigDecimal>,
    ): List<OverviewDashboardDetail> {
        val byWallet = rawGoalCommittedDetails.groupBy { it.walletItemId }
        val result = mutableListOf<OverviewDashboardDetail>()

        for (bank in visibleBankAccounts) {
            val walletId = bank.id!!
            val raws = byWallet[walletId].orEmpty()
            if (raws.isEmpty()) {
                continue
            }

            val children =
                raws
                    .map { raw ->
                        OverviewDashboardDetail(
                            sourceId = raw.sourceId,
                            sourceType = OverviewDashboardDetailSourceType.GOAL,
                            label = raw.label,
                            value = convertedValueByKey.getOrDefault(raw.key, BigDecimal.ZERO).asMoney(),
                        )
                    }.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
                    .sortedWith(compareByDescending<OverviewDashboardDetail> { it.value.abs() }.thenBy { it.label.lowercase() })

            if (children.isEmpty()) {
                continue
            }

            val parentValue = children.fold(BigDecimal.ZERO) { acc, child -> acc.add(child.value) }.asMoney()
            result.add(
                OverviewDashboardDetail(
                    sourceId = walletId,
                    sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                    label = bank.name,
                    value = parentValue,
                    children = children,
                    accountOverCommitted = accountOverCommittedForWallet(bank, raws, rawBalanceByBankId),
                ),
            )
        }

        return result
    }

    internal fun buildFreeBalanceDetailsByWallet(
        balanceTotal: BigDecimal,
        balanceDetails: List<OverviewDashboardDetail>,
        rawGoalCommittedDetails: List<RawDetail>,
        convertedValueByKey: Map<String, BigDecimal>,
        visibleBankAccounts: List<BankAccount>,
        rawBalanceByBankId: Map<UUID, BigDecimal>,
    ): List<OverviewDashboardDetail> {
        val byWallet = rawGoalCommittedDetails.groupBy { it.walletItemId }
        val balanceByWalletId = balanceDetails.associateBy { it.sourceId }

        return buildList {
            add(
                OverviewDashboardDetail(
                    sourceId = null,
                    sourceType = OverviewDashboardDetailSourceType.FORMULA,
                    label = "financesPage.overviewPage.detail.formula.balance",
                    value = balanceTotal,
                ),
            )

            for (bank in visibleBankAccounts) {
                val walletId = bank.id!!
                val balanceConv = balanceByWalletId[walletId]?.value ?: BigDecimal.ZERO.asMoney()
                val raws = byWallet[walletId].orEmpty()
                val committedSumConv =
                    raws
                        .fold(BigDecimal.ZERO) { acc, raw ->
                            acc.add(convertedValueByKey.getOrDefault(raw.key, BigDecimal.ZERO))
                        }.asMoney()
                val freeOnAccount = balanceConv.subtract(committedSumConv).asMoney()
                val children =
                    raws
                        .map { raw ->
                            OverviewDashboardDetail(
                                sourceId = raw.sourceId,
                                sourceType = OverviewDashboardDetailSourceType.GOAL,
                                label = raw.label,
                                value =
                                    convertedValueByKey
                                        .getOrDefault(raw.key, BigDecimal.ZERO)
                                        .asMoney()
                                        .negate()
                                        .asMoney(),
                            )
                        }.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
                        .sortedWith(compareByDescending<OverviewDashboardDetail> { it.value.abs() }.thenBy { it.label.lowercase() })

                add(
                    OverviewDashboardDetail(
                        sourceId = walletId,
                        sourceType = OverviewDashboardDetailSourceType.BANK_ACCOUNT,
                        label = bank.name,
                        value = freeOnAccount,
                        children = children,
                        accountOverCommitted = accountOverCommittedForWallet(bank, raws, rawBalanceByBankId),
                    ),
                )
            }
        }
    }

    private fun accountOverCommittedForWallet(
        bank: BankAccount,
        raws: List<RawDetail>,
        rawBalanceByBankId: Map<UUID, BigDecimal>,
    ): Boolean {
        val walletId = bank.id ?: return false
        val balance = rawBalanceByBankId[walletId] ?: BigDecimal.ZERO
        val nativeCommitted =
            raws
                .filter { it.currency.equals(bank.currency, ignoreCase = true) }
                .fold(BigDecimal.ZERO) { acc, raw -> acc.add(raw.value) }
                .asMoney()
        return nativeCommitted.compareTo(balance) > 0
    }
}
