package com.ynixt.sharedfinances.domain.services.simulation

import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationRequest
import com.ynixt.sharedfinances.domain.models.simulation.planning.PlanningSimulationResult
import java.util.UUID

data class PlanningSimulationContext(
    val ownerUserId: UUID?,
    val ownerGroupId: UUID?,
    val requestedByUserId: UUID,
)

interface PlanningSimulationEngine {
    suspend fun run(
        context: PlanningSimulationContext,
        request: PlanningSimulationRequest,
    ): PlanningSimulationResult
}
