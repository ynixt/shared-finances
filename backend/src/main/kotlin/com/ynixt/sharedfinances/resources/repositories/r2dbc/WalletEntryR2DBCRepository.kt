package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCursorFindAll
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletEntryR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository() {
    fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        cursor: WalletEntryCursorFindAll?,
    ): Flux<WalletEntryEntity> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        var sql = """
            select 
                we.*,
                ${WalletItemR2DBCMapping.createSelectForWalletItem("origin", "origin_")},
                ${WalletItemR2DBCMapping.createSelectForWalletItem("target", "target_")}
            from 
                wallet_entry we
            join wallet_item origin 
                on origin.id = we.origin_id
            left join wallet_item target 
                on target.id = we.target_id
            where
        """

        if (userId != null) sql += " we.user_id = :userId"
        if (groupId != null) sql += " we.group_id = :groupId"

        if (walletItemId != null) sql += " and (we.origin_id = :walletItemId or we.target_id = :walletItemId)"
        if (minimumDate != null) sql += " and we.date >= :minimumDate"
        if (maximumDate != null) sql += " and we.date <= :maximumDate"

        if (cursor != null) sql += " and (we.date, we.id) < (:cursorDate, :cursorId)"

        sql +=
            """
            ORDER BY we.date DESC, we.id DESC 
            LIMIT :limit
            """

        var spec =
            dbClient
                .sql(sql)

        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)
        if (walletItemId != null) spec = spec.bind("walletItemId", walletItemId)
        if (minimumDate != null) spec = spec.bind("minimumDate", minimumDate)
        if (maximumDate != null) spec = spec.bind("maximumDate", maximumDate)
        if (cursor != null) spec = spec.bind("cursorDate", cursor.maximumDate).bind("cursorId", cursor.maximumId)

        spec = spec.bind("limit", limit)

        return spec
            .map { row, _ ->
                WalletEntryR2DBCMapping.walletEntryFromRow(row, "").also {
                    it.origin = WalletItemR2DBCMapping.walletItemFromRow(row, "origin_")
                    it.target = WalletItemR2DBCMapping.walletItemFromRow(row, "target_")
                }
            }.all()
    }

    fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
    ): Flux<EntrySumResult> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        var sql = """
            SELECT
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE THEN we.value END), 0) AS balance,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE THEN GREATEST(we.value, 0) END), 0) AS revenue,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE THEN ABS(LEAST(we.value, 0)) END), 0) AS expense,

                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND we.date >= :minimumDate THEN we.value END), 0) AS period_balance,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND we.date >= :minimumDate THEN GREATEST(we.value, 0) END), 0) AS period_revenue,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND we.date >= :minimumDate THEN ABS(LEAST(we.value, 0)) END), 0) AS period_expense,

                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN we.value END), 0) AS projected_balance,
                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN GREATEST(we.value, 0) END), 0) AS projected_revenue,
                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN ABS(LEAST(we.value, 0)) END), 0) AS projected_expense,
                
                we.origin_id,
                null AS origin_bill_id
            from 
                wallet_entry we
            where
        """

        if (userId != null) sql += " we.user_id = :userId"
        if (groupId != null) sql += " we.group_id = :groupId"

        if (walletItemId != null) {
            sql += " and we.origin_id = :walletItemId"
        } else {
            sql += " and we.origin_bill_id is null and we.target_bill_id is null"
        }

        if (maximumDate != null) sql += " and we.date <= :maximumDate"

        sql += " GROUP BY we.origin_id"

        val flux =
            createFluxForSum(
                sql = sql,
                userId = userId,
                groupId = groupId,
                walletItemId = walletItemId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
                walletItemIdColumn = "origin_id",
                creditCardBillIdColumn = "origin_bill_id",
            )

        val fluxForTarget =
            if (walletItemId != null) {
                val sqlForTargetId =
                    sql
                        .replace("origin_id", "target_id")
                        .replace("we.value", "(we.value * -1)")

                createFluxForSum(
                    sql = sqlForTargetId,
                    userId = userId,
                    groupId = groupId,
                    walletItemId = walletItemId,
                    minimumDate = minimumDate,
                    maximumDate = maximumDate,
                    walletItemIdColumn = "target_id",
                    creditCardBillIdColumn = "target_bill_id",
                )
            } else {
                Flux.empty()
            }

        return Flux.concat(flux, fluxForTarget)
    }

    fun sumForCreditCardSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID,
        maximumDate: LocalDate,
        billId: UUID,
    ): Flux<EntrySumResult> {
        require(userId != null || groupId != null) { "Either userId or groupId must be provided" }

        var sql = """
            SELECT
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE THEN we.value END), 0) AS balance,
                0 AS revenue,
                0 AS expense,

                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND ccb.id = :billId THEN we.value END), 0) AS period_balance,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND ccb.id = :billId THEN GREATEST(we.value, 0) END), 0) AS period_revenue,
                COALESCE(SUM(CASE WHEN we.date <= CURRENT_DATE AND ccb.id = :billId THEN ABS(LEAST(we.value, 0)) END), 0) AS period_expense,

                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN we.value END), 0) AS projected_balance,
                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN GREATEST(we.value, 0) END), 0) AS projected_revenue,
                COALESCE(SUM(CASE WHEN we.date > CURRENT_DATE THEN ABS(LEAST(we.value, 0)) END), 0) AS projected_expense,
                
                we.origin_id,
                null AS origin_bill_id
            from 
                wallet_entry we
            join credit_card_bill ccb on ccb.id = we.origin_bill_id
            where 
                ccb.closing_date <= :maximumDate
                and we.origin_id = :walletItemId
        """

        if (userId != null) sql += " and we.user_id = :userId"
        if (groupId != null) sql += " and we.group_id = :groupId"

        sql += " GROUP BY we.origin_id"

        val flux =
            createFluxForSum(
                sql = sql,
                userId = userId,
                groupId = groupId,
                walletItemId = walletItemId,
                minimumDate = null,
                maximumDate = maximumDate,
                walletItemIdColumn = "origin_id",
                creditCardBillIdColumn = "origin_bill_id",
                billId = billId,
            )

        val sqlForTargetId =
            sql
                .replace("origin_id", "target_id")
                .replace("origin_bill_id", "target_bill_id")
                .replace("we.value", "(we.value * -1)")

        val fluxForTarget =
            createFluxForSum(
                sql = sqlForTargetId,
                userId = userId,
                groupId = groupId,
                walletItemId = walletItemId,
                minimumDate = null,
                maximumDate = maximumDate,
                walletItemIdColumn = "target_id",
                creditCardBillIdColumn = "target_bill_id",
                billId = billId,
            )

        return Flux.concat(flux, fluxForTarget)
    }

    private fun createFluxForSum(
        sql: String,
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        walletItemIdColumn: String,
        creditCardBillIdColumn: String,
        billId: UUID? = null,
    ): Flux<EntrySumResult> {
        var spec =
            dbClient
                .sql(sql)

        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)
        if (walletItemId != null) spec = spec.bind("walletItemId", walletItemId)
        if (minimumDate != null) spec = spec.bind("minimumDate", minimumDate)
        if (maximumDate != null) spec = spec.bind("maximumDate", maximumDate)
        if (billId != null) spec = spec.bind("billId", billId)

        return spec
            .map { row, _ ->
                EntrySumResult(
                    sum =
                        EntrySum(
                            balance = row.get("balance", BigDecimal::class.java)!!,
                            revenue = row.get("revenue", BigDecimal::class.java)!!,
                            expense = row.get("expense", BigDecimal::class.java)!!,
                        ),
                    period =
                        EntrySum(
                            balance = row.get("period_balance", BigDecimal::class.java)!!,
                            revenue = row.get("period_revenue", BigDecimal::class.java)!!,
                            expense = row.get("period_expense", BigDecimal::class.java)!!,
                        ),
                    projected =
                        EntrySum(
                            balance = row.get("projected_balance", BigDecimal::class.java)!!,
                            revenue = row.get("projected_revenue", BigDecimal::class.java)!!,
                            expense = row.get("projected_expense", BigDecimal::class.java)!!,
                        ),
                    walletItemId = row.get(walletItemIdColumn, UUID::class.java)!!,
                    creditCardBillId = row.get(creditCardBillIdColumn, UUID::class.java),
                )
            }.all()
    }
}
