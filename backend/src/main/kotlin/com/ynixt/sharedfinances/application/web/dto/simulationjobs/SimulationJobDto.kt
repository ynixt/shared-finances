package com.ynixt.sharedfinances.application.web.dto.simulationjobs

import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import java.time.OffsetDateTime
import java.util.UUID

data class SimulationJobDto(
    val id: UUID,
    val type: SimulationJobType,
    val status: SimulationJobStatus,
    val requestPayload: String?,
    val resultPayload: String?,
    val errorMessage: String?,
    val retries: Int,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val cancelledAt: OffsetDateTime?,
)
