package com.ynixt.sharedfinances.application.web.dto.simulationjobs

import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import java.time.OffsetDateTime
import java.util.UUID

data class SimulationJobStatusEventDto(
    val id: UUID,
    val type: SimulationJobType,
    val status: SimulationJobStatus,
    val resultPayload: String?,
    val errorMessage: String?,
    val finishedAt: OffsetDateTime?,
    val cancelledAt: OffsetDateTime?,
    val retries: Int,
)
