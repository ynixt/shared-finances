package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.models.WalletItem

data class EntrySummary(
    val total: EntrySum,
    val totalProjected: EntrySum,
    val totalPeriod: EntrySum,
    val grouped: List<EntrySummaryGrouped>,
)

data class EntrySummaryGrouped(
    val walletItem: WalletItem,
    val entries: List<EntrySummaryGroupedResult>,
)

data class EntrySummaryGroupedResult(
    val sum: EntrySum,
    val projected: EntrySum,
    val period: EntrySum,
)
