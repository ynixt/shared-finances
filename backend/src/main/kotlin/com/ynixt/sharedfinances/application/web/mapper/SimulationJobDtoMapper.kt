package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.simulationjobs.SimulationJobDto
import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity

interface SimulationJobDtoMapper {
    fun toDto(from: SimulationJobEntity): SimulationJobDto
}
