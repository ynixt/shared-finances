package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemForEntryListDto
import java.math.BigDecimal

data class EntrySummaryDto(
    val total: EntrySumDto,
    val totalProjected: EntrySumDto,
    val totalPeriod: EntrySumDto,
    val grouped: List<EntrySummaryGroupedDto>,
)

data class EntrySummaryGroupedDto(
    val walletItem: WalletItemForEntryListDto,
    val entries: List<EntrySummaryGroupedResultDto>,
)

data class EntrySummaryGroupedResultDto(
    val sum: EntrySumDto,
    val projected: EntrySumDto,
    val period: EntrySumDto,
)

data class EntrySumDto(
    val balance: BigDecimal,
    val revenue: BigDecimal,
    val expense: BigDecimal,
)
