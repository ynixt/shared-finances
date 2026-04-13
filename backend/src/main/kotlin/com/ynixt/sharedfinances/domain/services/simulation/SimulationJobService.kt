package com.ynixt.sharedfinances.domain.services.simulation

import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

data class NewSimulationJobInput(
    val type: SimulationJobType = SimulationJobType.PLANNING_SIMULATION,
    val requestPayload: String? = null,
)

interface SimulationJobService {
    suspend fun create(
        ownerUserId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity

    suspend fun createForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity

    suspend fun getForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity

    suspend fun getForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity

    suspend fun listForOwner(
        ownerUserId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity>

    suspend fun listForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity>

    suspend fun cancelForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity

    suspend fun cancelForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity

    suspend fun processDispatchMessage(jobId: UUID)

    suspend fun dispatchNextQueuedForOwner(ownerUserId: UUID)

    suspend fun dispatchNextQueuedForGroup(ownerGroupId: UUID)

    suspend fun reconcileExpiredLeases(): Long

    suspend fun purgeOldJobs(): Long
}
