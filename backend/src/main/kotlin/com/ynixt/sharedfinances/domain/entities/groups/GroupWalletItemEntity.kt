package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("group_wallet_item")
class GroupWalletItemEntity(
    val groupId: UUID,
    val walletItemId: UUID,
) : SimpleEntity()
