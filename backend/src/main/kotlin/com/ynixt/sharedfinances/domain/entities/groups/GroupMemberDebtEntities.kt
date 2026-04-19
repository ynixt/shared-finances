package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("group_member_debt_monthly")
class GroupMemberDebtMonthlyEntity(
    val groupId: UUID,
    val payerId: UUID,
    val receiverId: UUID,
    val month: LocalDate,
    val currency: String,
    val balance: BigDecimal,
) : AuditedEntity()

@Table("group_member_debt_movement")
class GroupMemberDebtMovementEntity(
    val groupId: UUID,
    val payerId: UUID,
    val receiverId: UUID,
    val month: LocalDate,
    val currency: String,
    val deltaSigned: BigDecimal,
    val reasonKind: GroupDebtMovementReasonKind,
    val createdByUserId: UUID,
    val note: String? = null,
    val sourceWalletEventId: UUID? = null,
    val sourceMovementId: UUID? = null,
) : AuditedEntity()
