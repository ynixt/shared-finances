package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.simulationjobs.CreateSimulationJobRequestDto
import com.ynixt.sharedfinances.application.web.dto.simulationjobs.SimulationJobDto
import com.ynixt.sharedfinances.application.web.mapper.SimulationJobDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.simulation.NewSimulationJobInput
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/simulation-jobs")
@Tag(name = "Simulation jobs", description = "Async planning simulation jobs lifecycle")
class SimulationJobController(
    private val simulationJobService: SimulationJobService,
    private val simulationJobDtoMapper: SimulationJobDtoMapper,
) {
    @Operation(summary = "Create async simulation job")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @Valid @RequestBody(required = false) body: CreateSimulationJobRequestDto?,
    ): SimulationJobDto {
        val request = body ?: CreateSimulationJobRequestDto()
        return simulationJobDtoMapper.toDto(
            simulationJobService.create(
                ownerUserId = principalToken.principal.id,
                input =
                    NewSimulationJobInput(
                        type = request.type,
                        requestPayload = request.requestPayload,
                    ),
            ),
        )
    }

    @Operation(summary = "List logged user simulation jobs")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Page<SimulationJobDto> =
        simulationJobService
            .listForOwner(
                ownerUserId = principalToken.principal.id,
                pageable = pageable,
            ).map(simulationJobDtoMapper::toDto)

    @Operation(summary = "Get simulation job by id")
    @GetMapping("/{jobId}")
    suspend fun getOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable jobId: UUID,
    ): SimulationJobDto =
        simulationJobDtoMapper.toDto(
            simulationJobService.getForOwner(
                ownerUserId = principalToken.principal.id,
                jobId = jobId,
            ),
        )

    @Operation(summary = "Cancel simulation job")
    @PostMapping("/{jobId}/cancel")
    suspend fun cancel(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable jobId: UUID,
    ): SimulationJobDto =
        simulationJobDtoMapper.toDto(
            simulationJobService.cancelForOwner(
                ownerUserId = principalToken.principal.id,
                jobId = jobId,
            ),
        )
}
