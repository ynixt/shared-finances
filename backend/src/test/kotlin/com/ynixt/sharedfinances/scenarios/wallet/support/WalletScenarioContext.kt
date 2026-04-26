package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.dashboard.GroupOverviewDashboard
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewDashboard
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.services.walletentry.TransferQuoteResult
import com.ynixt.sharedfinances.domain.services.walletentry.TransferRateResult
import com.ynixt.sharedfinances.scenarios.support.ScenarioContext
import java.time.YearMonth
import java.util.UUID

internal class WalletScenarioContext(
    currentUserId: UUID? = null,
    currentCurrency: String = "USD",
    var currentBankAccountId: UUID? = null,
    var currentCreditCardId: UUID? = null,
    var lastBillId: UUID? = null,
    var lastWalletEventId: UUID? = null,
    var lastRecurrenceConfigId: UUID? = null,
    var lastFetchedWalletEvent: EventListResponse? = null,
    var lastFetchedScheduledWalletEvent: EventListResponse? = null,
    var lastOverview: OverviewDashboard? = null,
    var lastGroupOverview: GroupOverviewDashboard? = null,
    var lastGroupFeedRequest: GroupFeedRequest? = null,
    var lastGroupFeedPage: CursorPage<EventListResponse>? = null,
    var lastTransferQuote: TransferQuoteResult? = null,
    var lastTransferRate: TransferRateResult? = null,
) : ScenarioContext(
        currentUserId = currentUserId,
        currentCurrency = currentCurrency,
    )

internal data class GroupFeedRequest(
    val groupId: UUID,
    val selectedMonth: YearMonth,
    val pageSize: Int,
    val memberIds: Set<UUID>,
    val bankAccountIds: Set<UUID>,
    val creditCardIds: Set<UUID>,
    val entryTypes: Set<WalletEntryType>,
)
