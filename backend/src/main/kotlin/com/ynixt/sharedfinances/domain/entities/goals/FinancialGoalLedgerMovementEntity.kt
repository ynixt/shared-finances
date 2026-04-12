package com.ynixt.sharedfinances.domain.entities.goals

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.GoalLedgerMovementKind
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("financial_goal_ledger_movement")
class FinancialGoalLedgerMovementEntity(
    val financialGoalId: UUID,
    val walletItemId: UUID,
    val signedAmount: BigDecimal,
    val note: String?,
    val movementKind: GoalLedgerMovementKind,
    val scheduleId: UUID?,
    /** Business date of the allocation (distinct from audit timestamps). */
    val movementDate: LocalDate,
) : AuditedEntity()
