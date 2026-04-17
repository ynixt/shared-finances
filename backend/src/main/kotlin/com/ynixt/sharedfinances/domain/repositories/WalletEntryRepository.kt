package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedBankFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedExpenseFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface WalletEntryRepository : EntityRepository<WalletEntryEntity> {
    fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEntryEntity>

    fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int>

    fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
        asOfDate: LocalDate,
    ): Flux<EntrySumResult>

    fun summarizeBankAccountsByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<BankAccountMonthlySummary>

    fun summarizeOverviewBankFacts(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExecutedBankFactSummary>

    fun summarizeOverviewExpenseByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseMonthlySummary>

    fun summarizeOverviewExpenseFacts(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExecutedExpenseFactSummary>

    fun summarizeOverviewExpenseBySource(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseSourceSummary>

    fun summarizeOverviewCashBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewCashBreakdownSummary>

    fun summarizeOverviewExpenseBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseBreakdownSummary>
}
