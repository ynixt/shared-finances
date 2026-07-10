package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtForbiddenException
import com.ynixt.sharedfinances.domain.exceptions.http.GroupDebtMovementNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidDebtSettlementException
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtSettlementDeletionService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GroupDebtSettlementDeletionServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val movementRepository: GroupMemberDebtMovementSpringDataRepository,
    private val walletEntryRemovalService: WalletEntryRemovalService,
    private val ledgerMaintenanceService: GroupDebtLedgerMaintenanceService,
) : GroupDebtSettlementDeletionService {
    @Transactional
    override suspend fun deleteSettlement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ) {
        ensureMutationAccess(userId, groupId)

        val selectedMovement =
            movementRepository
                .findByIdAndGroupId(movementId, groupId)
                .awaitSingleOrNull() ?: throw GroupDebtMovementNotFoundException(movementId)

        if (
            selectedMovement.reasonKind != GroupDebtMovementReasonKind.DEBT_SETTLEMENT ||
            selectedMovement.sourceWalletEventId == null
        ) {
            throw InvalidDebtSettlementException("Only executed debt settlement movements can be deleted")
        }

        val sourceWalletEventId = selectedMovement.sourceWalletEventId
        val sourceMovements =
            movementRepository
                .findAllBySourceWalletEventId(sourceWalletEventId)
                .asFlow()
                .toList()
                .filter { movement -> movement.groupId == groupId }

        if (sourceMovements.none { movement -> movement.id == selectedMovement.id }) {
            throw InvalidDebtSettlementException("The selected movement is not an active debt settlement fragment")
        }

        if (walletEntryRemovalService.deleteOneOffWithoutDebtRollback(userId = userId, walletEventId = sourceWalletEventId) == null) {
            throw InvalidDebtSettlementException("The linked settlement entry could not be deleted")
        }

        ledgerMaintenanceService.deleteMovements(sourceMovements)
        ledgerMaintenanceService.reconcileScopes(sourceMovements.map(ledgerMaintenanceService::scopeFor))
    }

    private suspend fun ensureMutationAccess(
        userId: UUID,
        groupId: UUID,
    ) {
        if (!groupPermissionService.hasPermission(userId = userId, groupId = groupId, permission = GroupPermissions.SEND_ENTRIES)) {
            throw GroupDebtForbiddenException()
        }
    }
}
