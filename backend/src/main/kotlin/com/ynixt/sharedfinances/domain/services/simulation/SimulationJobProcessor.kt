package com.ynixt.sharedfinances.domain.services.simulation

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity

interface SimulationJobProcessor {
    suspend fun process(job: SimulationJobEntity): String?
}
