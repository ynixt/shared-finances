package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("group_bank_account")
class GroupBankAccount(
    val groupId: UUID,
    val bankAccountId: UUID,
) : SimpleEntity()
