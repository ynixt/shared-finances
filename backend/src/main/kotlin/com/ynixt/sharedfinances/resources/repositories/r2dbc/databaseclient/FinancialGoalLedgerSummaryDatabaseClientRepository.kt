package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByGoalRow
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByWalletRow
import com.ynixt.sharedfinances.domain.repositories.GoalCurrencyCommittedRow
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.util.UUID

@Repository
class FinancialGoalLedgerSummaryDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : GoalLedgerCommittedSummaryRepository {
    override fun summarizeCommittedByUserGoals(userId: UUID): Flux<GoalCommittedByWalletRow> {
        val sql =
            """
            SELECT m.wallet_item_id, wi.currency, SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN financial_goal g ON g.id = m.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE wi.type = 'BANK_ACCOUNT'
            AND (
                g.user_id = :userId
                OR g.group_id IN (SELECT gu.group_id FROM group_user gu WHERE gu.user_id = :userId)
            )
            GROUP BY m.wallet_item_id, wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .map { row, _ ->
                GoalCommittedByWalletRow(
                    walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    committed = row.get("committed", BigDecimal::class.java)!!,
                )
            }.all()
    }

    override fun summarizeCommittedByUserGoalsDetailed(userId: UUID): Flux<GoalCommittedByGoalRow> {
        val sql =
            """
            SELECT g.id AS goal_id, g.name AS goal_name, m.wallet_item_id, wi.currency, SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN financial_goal g ON g.id = m.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE wi.type = 'BANK_ACCOUNT'
            AND (
                g.user_id = :userId
                OR g.group_id IN (SELECT gu.group_id FROM group_user gu WHERE gu.user_id = :userId)
            )
            GROUP BY g.id, g.name, m.wallet_item_id, wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .map { row, _ ->
                GoalCommittedByGoalRow(
                    goalId = row.get("goal_id", UUID::class.java)!!,
                    goalName = row.get("goal_name", String::class.java)!!,
                    walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    committed = row.get("committed", BigDecimal::class.java)!!,
                )
            }.all()
    }

    override fun summarizeCommittedByGroupGoals(groupId: UUID): Flux<GoalCommittedByWalletRow> {
        val sql =
            """
            SELECT m.wallet_item_id, wi.currency, SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN financial_goal g ON g.id = m.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE wi.type = 'BANK_ACCOUNT'
            AND g.group_id = :groupId
            GROUP BY m.wallet_item_id, wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                GoalCommittedByWalletRow(
                    walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    committed = row.get("committed", BigDecimal::class.java)!!,
                )
            }.all()
    }

    override fun summarizeCommittedByGroupGoalsDetailed(groupId: UUID): Flux<GoalCommittedByGoalRow> {
        val sql =
            """
            SELECT g.id AS goal_id, g.name AS goal_name, m.wallet_item_id, wi.currency, SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN financial_goal g ON g.id = m.financial_goal_id
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE wi.type = 'BANK_ACCOUNT'
            AND g.group_id = :groupId
            GROUP BY g.id, g.name, m.wallet_item_id, wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                GoalCommittedByGoalRow(
                    goalId = row.get("goal_id", UUID::class.java)!!,
                    goalName = row.get("goal_name", String::class.java)!!,
                    walletItemId = row.get("wallet_item_id", UUID::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    committed = row.get("committed", BigDecimal::class.java)!!,
                )
            }.all()
    }

    override fun summarizeCommittedByGoal(goalId: UUID): Flux<GoalCurrencyCommittedRow> {
        val sql =
            """
            SELECT wi.currency, SUM(m.signed_amount) AS committed
            FROM financial_goal_ledger_movement m
            INNER JOIN wallet_item wi ON wi.id = m.wallet_item_id
            WHERE m.financial_goal_id = :goalId
            GROUP BY wi.currency
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("goalId", goalId)
            .map { row, _ ->
                GoalCurrencyCommittedRow(
                    currency = row.get("currency", String::class.java)!!,
                    committed = row.get("committed", BigDecimal::class.java)!!,
                )
            }.all()
    }
}
