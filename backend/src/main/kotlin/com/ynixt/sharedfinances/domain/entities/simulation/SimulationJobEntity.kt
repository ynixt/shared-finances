package com.ynixt.sharedfinances.domain.entities.simulation

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("simulation_job")
class SimulationJobEntity(
    val ownerUserId: UUID?,
    val ownerGroupId: UUID?,
    val requestedByUserId: UUID,
    val type: SimulationJobType,
    val status: SimulationJobStatus,
    val requestPayload: String?,
    val resultPayload: String?,
    val errorMessage: String?,
    val leaseExpiresAt: OffsetDateTime?,
    val workerId: String?,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val cancelledAt: OffsetDateTime?,
    val retries: Int,
) : AuditedEntity()
