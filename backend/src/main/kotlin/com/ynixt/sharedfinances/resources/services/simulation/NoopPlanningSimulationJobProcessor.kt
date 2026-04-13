package com.ynixt.sharedfinances.resources.services.simulation

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobProcessor
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class NoopPlanningSimulationJobProcessor(
    private val clock: Clock,
) : SimulationJobProcessor {
    override suspend fun process(job: SimulationJobEntity): String? {
        // Keeps the async pipeline functional until the planning engine is implemented.
        delay(250)
        return """{"mode":"noop","jobId":"${job.id}","processedAt":"${OffsetDateTime.now(clock)}"}"""
    }
}
