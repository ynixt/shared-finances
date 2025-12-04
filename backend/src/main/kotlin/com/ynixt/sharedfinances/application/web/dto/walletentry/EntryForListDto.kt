package com.ynixt.sharedfinances.application.web.dto.walletentry

import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemForEntryListDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class EntryForListDto(
    val id: UUID,
    val type: WalletEntryType,
    val name: String?,
    val value: BigDecimal,
    val category: CategoryDto?,
    val user: UserSimpleDto?,
    val group: GroupDto?,
    val tags: List<String>?,
    val observations: String?,
    val origin: WalletItemForEntryListDto,
    val target: WalletItemForEntryListDto?,
    val date: LocalDate,
    val confirmed: Boolean,
    val installment: Int?,
    val recurrenceConfigId: UUID?,
)
