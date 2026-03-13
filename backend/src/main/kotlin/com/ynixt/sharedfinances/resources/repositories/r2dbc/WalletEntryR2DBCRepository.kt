package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
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
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE THEN we.value END), 0) AS balance,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE THEN GREATEST(we.value, 0) END), 0) AS revenue,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE THEN ABS(LEAST(we.value, 0)) END), 0) AS expense,

                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate THEN we.value END), 0) AS period_balance,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate THEN GREATEST(we.value, 0) END), 0) AS period_revenue,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate THEN ABS(LEAST(we.value, 0)) END), 0) AS period_expense,

                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE THEN we.value END), 0) AS projected_balance,
                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE THEN GREATEST(we.value, 0) END), 0) AS projected_revenue,
                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE THEN ABS(LEAST(we.value, 0)) END), 0) AS projected_expense,
                
                we.wallet_item_id,
                null AS bill_id
            from 
                wallet_entry we
            join wallet_event wev on wev.id = we.wallet_event_id
            where
        """

        if (userId != null) sql += " wev.user_id = :userId"
        if (groupId != null) sql += " wev.group_id = :groupId"

        if (walletItemId != null) {
            sql += " and we.wallet_item_id = :walletItemId"
        } else {
            sql += " and we.bill_id is null"
        }

        if (maximumDate != null) sql += " and wev.date <= :maximumDate"

        sql += " GROUP BY we.wallet_item_id"

        val flux =
            createFluxForSum(
                sql = sql,
                userId = userId,
                groupId = groupId,
                walletItemId = walletItemId,
                minimumDate = minimumDate,
                maximumDate = maximumDate,
                walletItemIdColumn = "wallet_item_id",
                creditCardBillIdColumn = "bill_id",
            )

        return flux
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
