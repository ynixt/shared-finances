package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.simulationjobs.SimulationJobDto
import com.ynixt.sharedfinances.application.web.mapper.SimulationJobDtoMapper
import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import org.springframework.stereotype.Component

@Component
class SimulationJobDtoMapperImpl : SimulationJobDtoMapper {
    override fun toDto(from: SimulationJobEntity): SimulationJobDto =
        SimulationJobDto(
            id = requireNotNull(from.id) { "simulation job id" },
            type = from.type,
            status = from.status,
            requestPayload = from.requestPayload,
            resultPayload = from.resultPayload,
            errorMessage = from.errorMessage,
            retries = from.retries,
            createdAt = from.createdAt,
            updatedAt = from.updatedAt,
            startedAt = from.startedAt,
            finishedAt = from.finishedAt,
            cancelledAt = from.cancelledAt,
        )
}
