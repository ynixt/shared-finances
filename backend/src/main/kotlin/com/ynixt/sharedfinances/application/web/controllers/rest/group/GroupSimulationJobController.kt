package com.ynixt.sharedfinances.application.web.controllers.rest.group

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
@RequestMapping("/groups/{groupId}/simulation-jobs")
@Tag(name = "Group simulation jobs", description = "Async planning simulation jobs lifecycle in group scope")
class GroupSimulationJobController(
    private val simulationJobService: SimulationJobService,
    private val simulationJobDtoMapper: SimulationJobDtoMapper,
) {
    @Operation(summary = "Create async simulation job for group")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @Valid @RequestBody(required = false) body: CreateSimulationJobRequestDto?,
    ): SimulationJobDto {
        val request = body ?: CreateSimulationJobRequestDto()
        return simulationJobDtoMapper.toDto(
            simulationJobService.createForGroup(
                requesterUserId = principalToken.principal.id,
                groupId = groupId,
                input =
                    NewSimulationJobInput(
                        type = request.type,
                        requestPayload = request.requestPayload,
                    ),
            ),
        )
    }

    @Operation(summary = "List group simulation jobs")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobDto> =
        simulationJobService
            .listForGroup(
                requesterUserId = principalToken.principal.id,
                groupId = groupId,
                pageable = pageable,
            ).map(simulationJobDtoMapper::toDto)

    @Operation(summary = "Get group simulation job by id")
    @GetMapping("/{jobId}")
    suspend fun getOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable jobId: UUID,
    ): SimulationJobDto =
        simulationJobDtoMapper.toDto(
            simulationJobService.getForGroup(
                requesterUserId = principalToken.principal.id,
                groupId = groupId,
                jobId = jobId,
            ),
        )

    @Operation(summary = "Cancel group simulation job")
    @PostMapping("/{jobId}/cancel")
    suspend fun cancel(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable jobId: UUID,
    ): SimulationJobDto =
        simulationJobDtoMapper.toDto(
            simulationJobService.cancelForGroup(
                requesterUserId = principalToken.principal.id,
                groupId = groupId,
                jobId = jobId,
            ),
        )
}
