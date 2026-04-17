package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedBankFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExecutedExpenseFactSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseSourceSummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.WalletEntryDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEntrySpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryRepositoryImpl(
    springDataRepository: WalletEntrySpringDataRepository,
    private val dcRepository: WalletEntryDatabaseClientRepository,
) : EntityRepositoryImpl<WalletEntrySpringDataRepository, WalletEntryEntity>(springDataRepository),
    WalletEntryRepository {
    override fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEntryEntity> =
        springDataRepository.findAllByWalletEventId(walletEventId)

    override fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int> = springDataRepository.deleteAllByWalletEventId(walletEventId)

    override fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
        asOfDate: LocalDate,
    ): Flux<EntrySumResult> =
        dcRepository.sumForBankAccountSummary(
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            asOfDate = asOfDate,
        )

    override fun summarizeBankAccountsByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<BankAccountMonthlySummary> =
        dcRepository.summarizeBankAccountsByMonth(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewBankFacts(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExecutedBankFactSummary> =
        dcRepository.summarizeOverviewBankFacts(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewExpenseByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseMonthlySummary> =
        dcRepository.summarizeOverviewExpenseByMonth(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewExpenseFacts(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExecutedExpenseFactSummary> =
        dcRepository.summarizeOverviewExpenseFacts(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewExpenseBySource(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseSourceSummary> =
        dcRepository.summarizeOverviewExpenseBySource(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewCashBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewCashBreakdownSummary> =
        dcRepository.summarizeOverviewCashBreakdown(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )

    override fun summarizeOverviewExpenseBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseBreakdownSummary> =
        dcRepository.summarizeOverviewExpenseBreakdown(
            userId = userId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )
}
