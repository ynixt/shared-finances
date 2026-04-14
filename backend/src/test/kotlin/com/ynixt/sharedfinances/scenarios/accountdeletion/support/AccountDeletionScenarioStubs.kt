package com.ynixt.sharedfinances.scenarios.accountdeletion.support

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.simulation.NewSimulationJobInput
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

internal class RecordingComplianceSimulationJobService : SimulationJobService {
    val complianceUserIds = mutableListOf<UUID>()

    override suspend fun cancelAndRemoveAllJobsLinkedToUserForCompliance(userId: UUID) {
        complianceUserIds.add(userId)
    }

    override suspend fun create(
        ownerUserId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity = unsupported()

    override suspend fun createForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        input: NewSimulationJobInput,
    ): SimulationJobEntity = unsupported()

    override suspend fun getForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity = unsupported()

    override suspend fun getForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity = unsupported()

    override suspend fun listForOwner(
        ownerUserId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity> = unsupported()

    override suspend fun listForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<SimulationJobEntity> = unsupported()

    override suspend fun cancelForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ): SimulationJobEntity = unsupported()

    override suspend fun cancelForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobEntity = unsupported()

    override suspend fun deleteForOwner(
        ownerUserId: UUID,
        jobId: UUID,
    ) {
        unsupported()
    }

    override suspend fun deleteForGroup(
        requesterUserId: UUID,
        groupId: UUID,
        jobId: UUID,
    ) {
        unsupported()
    }

    override suspend fun processDispatchMessage(jobId: UUID) {
        unsupported()
    }

    override suspend fun dispatchNextQueuedForOwner(ownerUserId: UUID) {
        unsupported()
    }

    override suspend fun dispatchNextQueuedForGroup(ownerGroupId: UUID) {
        unsupported()
    }

    override suspend fun reconcileExpiredLeases(): Long = unsupported()

    override suspend fun purgeOldJobs(): Long = unsupported()

    private fun unsupported(): Nothing = error("AccountDeletionScenario: SimulationJobService operation not stubbed")
}

internal object NoOpGroupActionEventServiceStub : GroupActionEventService {
    override suspend fun sendInsertedGroup(
        userId: UUID,
        group: GroupEntity,
    ) {}

    override suspend fun sendUpdatedGroup(
        userId: UUID,
        group: GroupWithRole,
    ) {}

    override suspend fun sendDeletedGroup(
        userId: UUID,
        id: UUID,
        membersId: List<UUID>,
    ) {}

    override suspend fun sendBankAssociated(
        userId: UUID,
        groupBankAccount: GroupWalletItemEntity,
    ) {}

    override suspend fun sendBankUnassociated(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ) {}

    override suspend fun sendCreditCardAssociated(
        userId: UUID,
        groupCreditCard: GroupWalletItemEntity,
    ) {}

    override suspend fun sendCreditCardUnassociated(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ) {}
}
