package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("group_credit_card")
class GroupCreditCard(
    val groupId: UUID,
    val creditCardId: UUID,
) : SimpleEntity()
