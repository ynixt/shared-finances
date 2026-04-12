package com.ynixt.sharedfinances.domain.entities.goals

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("financial_goal")
class FinancialGoalEntity(
    val name: String,
    val description: String?,
    val userId: UUID?,
    val groupId: UUID?,
    val deadline: LocalDate?,
) : AuditedEntity()
