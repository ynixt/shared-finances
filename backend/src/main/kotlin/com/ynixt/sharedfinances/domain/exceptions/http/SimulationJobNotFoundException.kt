package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class SimulationJobNotFoundException(
    jobId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.simulationJobNotFound",
        argsI18n = mapOf("jobId" to jobId),
        alternativeMessage = "Simulation job $jobId not found.",
    )
