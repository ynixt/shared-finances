package com.ynixt.sharedfinances.domain.models.walletentry

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class EntryListResponse(
    val id: UUID,
    val type: WalletEntryType,
    val name: String?,
    val value: BigDecimal,
    val category: WalletEntryCategoryEntity?,
    val user: UserEntity?,
    val group: GroupEntity?,
    val tags: List<String>?,
    val observations: String?,
    val origin: WalletItem,
    val target: WalletItem?,
    val date: LocalDate,
    val confirmed: Boolean,
    val installment: Int?,
    val recurrenceConfigId: UUID?,
    val currency: String,
)
