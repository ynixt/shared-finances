package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.models.dashboard.BankAccountMonthlySummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewCashDirection
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseBreakdownSummary
import com.ynixt.sharedfinances.domain.models.dashboard.OverviewExpenseMonthlySummary
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySum
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
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
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND NOT wev.initial_balance THEN GREATEST(we.value, 0) END), 0) AS revenue,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND NOT wev.initial_balance THEN ABS(LEAST(we.value, 0)) END), 0) AS expense,

                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate THEN we.value END), 0) AS period_balance,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate AND NOT wev.initial_balance THEN GREATEST(we.value, 0) END), 0) AS period_revenue,
                COALESCE(SUM(CASE WHEN wev.date <= CURRENT_DATE AND wev.date >= :minimumDate AND NOT wev.initial_balance THEN ABS(LEAST(we.value, 0)) END), 0) AS period_expense,

                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE THEN we.value END), 0) AS projected_balance,
                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE AND NOT wev.initial_balance THEN GREATEST(we.value, 0) END), 0) AS projected_revenue,
                COALESCE(SUM(CASE WHEN wev.date > CURRENT_DATE AND NOT wev.initial_balance THEN ABS(LEAST(we.value, 0)) END), 0) AS projected_expense,
                
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

    fun summarizeBankAccountsByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<BankAccountMonthlySummary> {
        val internalTransferPredicate =
            """
            COALESCE(
                wev.type = 'TRANSFER'
                AND counterparty_wi.type = 'BANK_ACCOUNT'
                AND counterparty_wi.user_id = :userId,
                FALSE
            )
            """.trimIndent()

        val sql =
            """
            SELECT
                we.wallet_item_id AS wallet_item_id,
                date_trunc('month', wev.date)::date AS month_date,
                COALESCE(SUM(we.value), 0) AS net,
                COALESCE(
                    SUM(
                        CASE
                            WHEN we.value > 0 AND NOT wev.initial_balance AND NOT ($internalTransferPredicate) THEN we.value
                            ELSE 0
                        END
                    ),
                    0
                ) AS cash_in,
                COALESCE(
                    SUM(
                        CASE
                            WHEN we.value < 0 AND NOT wev.initial_balance AND NOT ($internalTransferPredicate) THEN ABS(we.value)
                            ELSE 0
                        END
                    ),
                    0
                ) AS cash_out
            FROM wallet_entry we
            JOIN wallet_event wev ON wev.id = we.wallet_event_id
            JOIN wallet_item wi ON wi.id = we.wallet_item_id
            LEFT JOIN wallet_entry counterparty_we
                ON counterparty_we.wallet_event_id = we.wallet_event_id
                AND counterparty_we.id <> we.id
            LEFT JOIN wallet_item counterparty_wi ON counterparty_wi.id = counterparty_we.wallet_item_id
            WHERE
                wi.user_id = :userId
                AND wi.type = 'BANK_ACCOUNT'
                AND wi.enabled = true
                AND wi.show_on_dashboard = true
                AND wev.date >= :minimumDate
                AND wev.date <= :maximumDate
            GROUP BY
                we.wallet_item_id,
                date_trunc('month', wev.date)
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("minimumDate", minimumDate)
            .bind("maximumDate", maximumDate)
            .map { row, _ ->
                val monthDate = row.get("month_date", LocalDate::class.java)!!

                BankAccountMonthlySummary(
                    walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                    month = java.time.YearMonth.from(monthDate),
                    net = row.get("net", BigDecimal::class.java)!!,
                    cashIn = row.get("cash_in", BigDecimal::class.java)!!,
                    cashOut = row.get("cash_out", BigDecimal::class.java)!!,
                )
            }.all()
    }

    fun summarizeOverviewExpenseByMonth(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseMonthlySummary> {
        val sql =
            """
            SELECT
                date_trunc('month', wev.date)::date AS month_date,
                wi.currency AS currency,
                COALESCE(SUM(ABS(LEAST(we.value, 0))), 0) AS expense
            FROM wallet_entry we
            JOIN wallet_event wev ON wev.id = we.wallet_event_id
            JOIN wallet_item wi ON wi.id = we.wallet_item_id
            WHERE
                wi.user_id = :userId
                AND wi.type IN ('BANK_ACCOUNT', 'CREDIT_CARD')
                AND wi.enabled = true
                AND wi.show_on_dashboard = true
                AND NOT wev.initial_balance
                AND wev.type = 'EXPENSE'
                AND we.value < 0
                AND wev.date >= :minimumDate
                AND wev.date <= :maximumDate
            GROUP BY
                date_trunc('month', wev.date),
                wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("minimumDate", minimumDate)
            .bind("maximumDate", maximumDate)
            .map { row, _ ->
                val monthDate = row.get("month_date", LocalDate::class.java)!!

                OverviewExpenseMonthlySummary(
                    month = java.time.YearMonth.from(monthDate),
                    currency = row.get("currency", String::class.java)!!,
                    expense = row.get("expense", BigDecimal::class.java)!!,
                )
            }.all()
    }

    fun summarizeOverviewCashBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewCashBreakdownSummary> {
        val internalTransferPredicate =
            """
            COALESCE(
                wev.type = 'TRANSFER'
                AND counterparty_wi.type = 'BANK_ACCOUNT'
                AND counterparty_wi.user_id = :userId,
                FALSE
            )
            """.trimIndent()

        val sql =
            """
            SELECT
                CASE
                    WHEN we.value >= 0 THEN 'IN'
                    ELSE 'OUT'
                END AS direction,
                wev.category_id AS category_id,
                cat.name AS category_name,
                wi.currency AS currency,
                COALESCE(SUM(ABS(we.value)), 0) AS amount
            FROM wallet_entry we
            JOIN wallet_event wev ON wev.id = we.wallet_event_id
            JOIN wallet_item wi ON wi.id = we.wallet_item_id
            LEFT JOIN wallet_entry counterparty_we
                ON counterparty_we.wallet_event_id = we.wallet_event_id
                AND counterparty_we.id <> we.id
            LEFT JOIN wallet_item counterparty_wi ON counterparty_wi.id = counterparty_we.wallet_item_id
            LEFT JOIN wallet_entry_category cat ON cat.id = wev.category_id
            WHERE
                wi.user_id = :userId
                AND wi.type = 'BANK_ACCOUNT'
                AND wi.enabled = true
                AND wi.show_on_dashboard = true
                AND NOT wev.initial_balance
                AND wev.date >= :minimumDate
                AND wev.date <= :maximumDate
                AND NOT ($internalTransferPredicate)
            GROUP BY
                direction,
                wev.category_id,
                cat.name,
                wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("minimumDate", minimumDate)
            .bind("maximumDate", maximumDate)
            .map { row, _ ->
                OverviewCashBreakdownSummary(
                    direction = OverviewCashDirection.valueOf(row.get("direction", String::class.java)!!),
                    categoryId = row.get("category_id", UUID::class.java),
                    categoryName = row.get("category_name", String::class.java),
                    currency = row.get("currency", String::class.java)!!,
                    amount = row.get("amount", BigDecimal::class.java)!!,
                )
            }.all()
    }

    fun summarizeOverviewExpenseBreakdown(
        userId: UUID,
        minimumDate: LocalDate,
        maximumDate: LocalDate,
    ): Flux<OverviewExpenseBreakdownSummary> {
        val sql =
            """
            SELECT
                wev.group_id AS group_id,
                grp.name AS group_name,
                wev.category_id AS category_id,
                cat.name AS category_name,
                wi.currency AS currency,
                COALESCE(SUM(ABS(LEAST(we.value, 0))), 0) AS expense
            FROM wallet_entry we
            JOIN wallet_event wev ON wev.id = we.wallet_event_id
            JOIN wallet_item wi ON wi.id = we.wallet_item_id
            LEFT JOIN "group" grp ON grp.id = wev.group_id
            LEFT JOIN wallet_entry_category cat ON cat.id = wev.category_id
            WHERE
                wi.user_id = :userId
                AND wi.type IN ('BANK_ACCOUNT', 'CREDIT_CARD')
                AND wi.enabled = true
                AND wi.show_on_dashboard = true
                AND NOT wev.initial_balance
                AND wev.type = 'EXPENSE'
                AND we.value < 0
                AND wev.date >= :minimumDate
                AND wev.date <= :maximumDate
            GROUP BY
                wev.group_id,
                grp.name,
                wev.category_id,
                cat.name,
                wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("minimumDate", minimumDate)
            .bind("maximumDate", maximumDate)
            .map { row, _ ->
                OverviewExpenseBreakdownSummary(
                    groupId = row.get("group_id", UUID::class.java),
                    groupName = row.get("group_name", String::class.java),
                    categoryId = row.get("category_id", UUID::class.java),
                    categoryName = row.get("category_name", String::class.java),
                    currency = row.get("currency", String::class.java)!!,
                    expense = row.get("expense", BigDecimal::class.java)!!,
                )
            }.all()
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
