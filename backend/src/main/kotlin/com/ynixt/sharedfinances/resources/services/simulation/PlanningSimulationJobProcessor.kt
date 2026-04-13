package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationRequest
import com.ynixt.sharedfinances.domain.services.simulation.PlanningSimulationContext
import com.ynixt.sharedfinances.domain.services.simulation.PlanningSimulationEngine
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobProcessor
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class PlanningSimulationJobProcessor(
    private val planningSimulationEngine: PlanningSimulationEngine,
    private val objectMapper: ObjectMapper,
) : SimulationJobProcessor {
    override suspend fun process(job: SimulationJobEntity): String {
        require((job.ownerUserId != null) xor (job.ownerGroupId != null)) {
            "Simulation job scope must have exactly one owner scope."
        }

        val request =
            if (job.requestPayload.isNullOrBlank()) {
                PlanningSimulationRequest()
            } else {
                objectMapper.readValue<PlanningSimulationRequest>(job.requestPayload)
            }

        val result =
            planningSimulationEngine.run(
                context =
                    PlanningSimulationContext(
                        ownerUserId = job.ownerUserId,
                        ownerGroupId = job.ownerGroupId,
                        requestedByUserId = job.requestedByUserId,
                    ),
                request = request,
            )

        return objectMapper.writeValueAsString(result)
    }
}
