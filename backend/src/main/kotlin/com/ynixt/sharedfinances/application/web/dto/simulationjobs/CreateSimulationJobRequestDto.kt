package com.ynixt.sharedfinances.application.web.dto.simulationjobs

import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import jakarta.validation.constraints.Size

data class CreateSimulationJobRequestDto(
    val type: SimulationJobType = SimulationJobType.PLANNING_SIMULATION,
    @field:Size(max = 200000)
    val requestPayload: String? = null,
)
