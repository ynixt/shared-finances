package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMonthlyCashFlow
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import java.time.YearMonth
import java.util.UUID

interface GroupDebtService {
    suspend fun applyWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
        entries: List<WalletEntryEntity>,
    )

    suspend fun rollbackWalletEvent(
        actorUserId: UUID,
        event: WalletEventEntity,
    )

    suspend fun getWorkspace(
        userId: UUID,
        groupId: UUID,
    ): GroupDebtWorkspace

    suspend fun listHistory(
        userId: UUID,
        groupId: UUID,
        filter: GroupDebtHistoryFilter = GroupDebtHistoryFilter(),
    ): List<GroupDebtMovementLine>

    suspend fun getMovement(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
    ): GroupDebtMovementLine

    suspend fun createManualAdjustment(
        userId: UUID,
        groupId: UUID,
        input: NewGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine

    suspend fun editManualAdjustment(
        userId: UUID,
        groupId: UUID,
        movementId: UUID,
        input: EditGroupDebtManualAdjustmentInput,
    ): GroupDebtMovementLine

    suspend fun loadMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: YearMonth,
        toMonth: YearMonth,
    ): Map<Pair<YearMonth, String>, GroupDebtMonthlyCashFlow>
}
