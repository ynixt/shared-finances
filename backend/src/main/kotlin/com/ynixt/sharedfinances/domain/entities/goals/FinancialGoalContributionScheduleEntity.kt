package com.ynixt.sharedfinances.domain.entities.goals

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("financial_goal_contribution_schedule")
class FinancialGoalContributionScheduleEntity(
    val financialGoalId: UUID,
    val walletItemId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val periodicity: RecurrenceType,
    val qtyExecuted: Int,
    val qtyLimit: Int?,
    val lastExecution: LocalDate?,
    val nextExecution: LocalDate?,
    val endExecution: LocalDate?,
    val removesAllocation: Boolean,
) : AuditedEntity()
