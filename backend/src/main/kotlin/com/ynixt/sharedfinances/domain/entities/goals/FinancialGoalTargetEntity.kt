package com.ynixt.sharedfinances.domain.entities.goals

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

@Table("financial_goal_target")
class FinancialGoalTargetEntity(
    val financialGoalId: UUID,
    val currency: String,
    val targetAmount: BigDecimal,
) : AuditedEntity()
