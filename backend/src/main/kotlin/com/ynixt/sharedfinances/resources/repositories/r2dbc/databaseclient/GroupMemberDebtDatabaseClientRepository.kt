package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.groups.GroupMemberDebtMovementEntity
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Repository
class GroupMemberDebtDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    fun upsertMonthlyDelta(movement: GroupMemberDebtMovementEntity): Mono<Void> {
        val sql =
            """
            INSERT INTO group_member_debt_monthly (
                id,
                group_id,
                payer_id,
                receiver_id,
                month,
                currency,
                balance
            ) VALUES (
                :id,
                :groupId,
                :payerId,
                :receiverId,
                :month,
                :currency,
                :balance
            )
            ON CONFLICT (group_id, payer_id, receiver_id, month, currency)
            DO UPDATE SET
                balance = group_member_debt_monthly.balance + EXCLUDED.balance,
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("id", UUID.randomUUID())
            .bind("groupId", movement.groupId)
            .bind("payerId", movement.payerId)
            .bind("receiverId", movement.receiverId)
            .bind("month", movement.month)
            .bind("currency", movement.currency.uppercase())
            .bind("balance", movement.deltaSigned)
            .then()
    }

    fun listMonthlyComposition(groupId: UUID): Flux<DebtMonthlyCompositionRow> {
        val sql =
            """
            SELECT
                monthly.payer_id,
                monthly.receiver_id,
                monthly.currency,
                monthly.month,
                monthly.balance AS net_amount,
                COALESCE(movement_agg.charge_delta, 0) AS charge_delta,
                COALESCE(movement_agg.settlement_delta, 0) AS settlement_delta,
                COALESCE(movement_agg.manual_adjustment_delta, 0) AS manual_adjustment_delta
            FROM group_member_debt_monthly monthly
            LEFT JOIN (
                SELECT
                    group_id,
                    payer_id,
                    receiver_id,
                    currency,
                    month,
                    SUM(
                        CASE
                            WHEN reason_kind IN ('BENEFICIARY_CHARGE', 'BENEFICIARY_REVERSAL')
                                THEN delta_signed
                            ELSE 0
                        END
                    ) AS charge_delta,
                    SUM(
                        CASE
                            WHEN reason_kind IN ('DEBT_SETTLEMENT', 'DEBT_SETTLEMENT_REVERSAL')
                                THEN delta_signed
                            ELSE 0
                        END
                    ) AS settlement_delta,
                    SUM(
                        CASE
                            WHEN reason_kind IN ('MANUAL_ADJUSTMENT', 'MANUAL_ADJUSTMENT_COMPENSATION')
                                THEN delta_signed
                            ELSE 0
                        END
                    ) AS manual_adjustment_delta
                FROM group_member_debt_movement
                WHERE group_id = :groupId
                GROUP BY group_id, payer_id, receiver_id, currency, month
            ) movement_agg
                ON movement_agg.group_id = monthly.group_id
                AND movement_agg.payer_id = monthly.payer_id
                AND movement_agg.receiver_id = monthly.receiver_id
                AND movement_agg.currency = monthly.currency
                AND movement_agg.month = monthly.month
            WHERE monthly.group_id = :groupId
            ORDER BY monthly.payer_id, monthly.receiver_id, monthly.currency, monthly.month
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                DebtMonthlyCompositionRow(
                    payerId = row.get("payer_id", UUID::class.java)!!,
                    receiverId = row.get("receiver_id", UUID::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    month = YearMonth.from(row.get("month", LocalDate::class.java)!!),
                    netAmount = row.get("net_amount", BigDecimal::class.java)!!,
                    chargeDelta = row.get("charge_delta", BigDecimal::class.java)!!,
                    settlementDelta = row.get("settlement_delta", BigDecimal::class.java)!!,
                    manualAdjustmentDelta = row.get("manual_adjustment_delta", BigDecimal::class.java)!!,
                )
            }.all()
    }

    fun listHistory(
        groupId: UUID,
        payerId: UUID?,
        receiverId: UUID?,
        currency: String?,
    ): Flux<DebtMovementHistoryRow> {
        var sql =
            """
            SELECT
                id,
                payer_id,
                receiver_id,
                month,
                currency,
                delta_signed,
                reason_kind,
                created_by_user_id,
                note,
                source_wallet_event_id,
                source_movement_id,
                created_at
            FROM group_member_debt_movement
            WHERE group_id = :groupId
            """.trimIndent()

        if (payerId != null) {
            sql += " AND payer_id = :payerId"
        }
        if (receiverId != null) {
            sql += " AND receiver_id = :receiverId"
        }
        if (!currency.isNullOrBlank()) {
            sql += " AND currency = :currency"
        }

        sql += " ORDER BY created_at ASC, id ASC"

        var spec = dbClient.sql(sql).bind("groupId", groupId)
        if (payerId != null) {
            spec = spec.bind("payerId", payerId)
        }
        if (receiverId != null) {
            spec = spec.bind("receiverId", receiverId)
        }
        if (!currency.isNullOrBlank()) {
            spec = spec.bind("currency", currency.uppercase())
        }

        return spec
            .map { row, _ ->
                DebtMovementHistoryRow(
                    id = row.get("id", UUID::class.java)!!,
                    payerId = row.get("payer_id", UUID::class.java)!!,
                    receiverId = row.get("receiver_id", UUID::class.java)!!,
                    month = YearMonth.from(row.get("month", LocalDate::class.java)!!),
                    currency = row.get("currency", String::class.java)!!,
                    deltaSigned = row.get("delta_signed", BigDecimal::class.java)!!,
                    reasonKind = row.get("reason_kind", String::class.java)!!,
                    createdByUserId = row.get("created_by_user_id", UUID::class.java)!!,
                    note = row.get("note", String::class.java),
                    sourceWalletEventId = row.get("source_wallet_event_id", UUID::class.java),
                    sourceMovementId = row.get("source_movement_id", UUID::class.java),
                    createdAt = row.get("created_at", java.time.OffsetDateTime::class.java),
                )
            }.all()
    }

    fun summarizeMonthlyCashFlow(
        groupId: UUID,
        scopedUserIds: Set<UUID>,
        fromMonth: LocalDate,
        toMonth: LocalDate,
    ): Flux<DebtMonthlyCashFlowRow> {
        if (scopedUserIds.isEmpty()) {
            return Flux.empty()
        }

        val sql =
            """
            SELECT
                month,
                currency,
                COALESCE(SUM(CASE WHEN payer_id = ANY(:userIds) THEN balance ELSE 0 END), 0) AS debt_outflow,
                COALESCE(SUM(CASE WHEN receiver_id = ANY(:userIds) THEN balance ELSE 0 END), 0) AS debt_inflow
            FROM group_member_debt_monthly
            WHERE
                group_id = :groupId
                AND month >= :fromMonth
                AND month <= :toMonth
            GROUP BY month, currency
            ORDER BY month ASC, currency ASC
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("userIds", scopedUserIds.toTypedArray())
            .bind("fromMonth", fromMonth)
            .bind("toMonth", toMonth)
            .map { row, _ ->
                DebtMonthlyCashFlowRow(
                    month = YearMonth.from(row.get("month", LocalDate::class.java)!!),
                    currency = row.get("currency", String::class.java)!!,
                    debtOutflow = row.get("debt_outflow", BigDecimal::class.java)!!,
                    debtInflow = row.get("debt_inflow", BigDecimal::class.java)!!,
                )
            }.all()
    }

    data class DebtMonthlyCompositionRow(
        val payerId: UUID,
        val receiverId: UUID,
        val currency: String,
        val month: YearMonth,
        val netAmount: BigDecimal,
        val chargeDelta: BigDecimal,
        val settlementDelta: BigDecimal,
        val manualAdjustmentDelta: BigDecimal,
    )

    data class DebtMovementHistoryRow(
        val id: UUID,
        val payerId: UUID,
        val receiverId: UUID,
        val month: YearMonth,
        val currency: String,
        val deltaSigned: BigDecimal,
        val reasonKind: String,
        val createdByUserId: UUID,
        val note: String?,
        val sourceWalletEventId: UUID?,
        val sourceMovementId: UUID?,
        val createdAt: java.time.OffsetDateTime?,
    )

    data class DebtMonthlyCashFlowRow(
        val month: YearMonth,
        val currency: String,
        val debtOutflow: BigDecimal,
        val debtInflow: BigDecimal,
    )
}
