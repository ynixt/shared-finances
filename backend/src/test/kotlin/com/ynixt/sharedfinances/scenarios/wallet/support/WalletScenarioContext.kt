package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.scenarios.support.ScenarioContext
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
) : ScenarioContext(
        currentUserId = currentUserId,
        currentCurrency = currentCurrency,
    )
