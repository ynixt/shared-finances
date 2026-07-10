package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupMemberDebtDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupMemberDebtMovementSpringDataRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class GroupDebtLedgerMaintenanceService(
    private val movementRepository: GroupMemberDebtMovementSpringDataRepository,
    private val debtDatabaseClientRepository: GroupMemberDebtDatabaseClientRepository,
) {
    suspend fun deleteMovements(movements: Collection<GroupMemberDebtMovementEntity>) {
        movements
            .mapNotNull { movement -> movement.id?.toString() }
            .distinct()
            .forEach { movementId ->
                movementRepository.deleteById(movementId).awaitSingleOrNull()
            }
    }

    suspend fun reconcileScopes(scopes: Collection<MonthlyDebtScope>) {
        scopes
            .distinct()
            .forEach { scope ->
                val balance =
                    debtDatabaseClientRepository
                        .sumMovementBalanceForScope(
                            groupId = scope.groupId,
                            payerId = scope.payerId,
                            receiverId = scope.receiverId,
                            month = scope.month,
                            currency = scope.currency,
                        ).awaitSingle()
                        .asMoney()

                if (balance.compareTo(BigDecimal.ZERO.asMoney()) == 0) {
                    debtDatabaseClientRepository
                        .deleteMonthlyBalance(
                            groupId = scope.groupId,
                            payerId = scope.payerId,
                            receiverId = scope.receiverId,
                            month = scope.month,
                            currency = scope.currency,
                        ).awaitSingle()
                } else {
                    debtDatabaseClientRepository
                        .upsertMonthlyBalance(
                            groupId = scope.groupId,
                            payerId = scope.payerId,
                            receiverId = scope.receiverId,
                            month = scope.month,
                            currency = scope.currency,
                            balance = balance,
                        ).awaitSingleOrNull()
                }
            }
    }

    fun scopeFor(movement: GroupMemberDebtMovementEntity): MonthlyDebtScope =
        MonthlyDebtScope(
            groupId = movement.groupId,
            payerId = movement.payerId,
            receiverId = movement.receiverId,
            month = movement.month,
            currency = movement.currency.uppercase(),
        )

    data class MonthlyDebtScope(
        val groupId: UUID,
        val payerId: UUID,
        val receiverId: UUID,
        val month: LocalDate,
        val currency: String,
    )

    private fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
