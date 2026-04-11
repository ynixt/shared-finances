package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class EventListResponse(
    val id: UUID?,
    val type: WalletEntryType,
    val name: String?,
    val category: WalletEntryCategoryEntity?,
    val user: UserEntity?,
    val group: GroupEntity?,
    val tags: List<String>?,
    val observations: String?,
    val date: LocalDate,
    val confirmed: Boolean,
    val installment: Int?,
    val recurrenceConfigId: UUID?,
    val recurrenceConfig: RecurrenceEventEntity?,
    val currency: String,
    val originValue: BigDecimal? = null,
    val targetValue: BigDecimal? = null,
    val entries: List<EntryResponse>,
) {
    data class EntryResponse(
        val value: BigDecimal,
        val walletItem: WalletItem,
        val walletItemId: UUID,
        val billDate: LocalDate?,
        val billId: UUID?,
        val contributionPercent: BigDecimal? = null,
    )
}
