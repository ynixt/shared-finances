package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class SimulationJobForbiddenException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(403),
        messageI18n = "apiErrors.simulationJobForbidden",
        alternativeMessage = "You are not allowed to access this simulation job.",
    )
