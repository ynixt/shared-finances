package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.BankAccountR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class GroupBankAccountR2DBCRepository(
    private val dbClient: DatabaseClient,
) {
    fun findAllAllowedForGroup(groupId: UUID): Flux<BankAccount> {
        val sql =
            """
            SELECT
              ba.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_user gu
            JOIN users u
              ON u.id = gu.user_id
            JOIN bank_account ba
              ON ba.user_id = u.id
            LEFT JOIN group_bank_account gba
              ON gba.group_id = gu.group_id
             AND gba.bank_account_id = ba.id
            WHERE
              gu.group_id = :groupId
              AND gba.bank_account_id IS NULL
            ORDER BY ba.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                BankAccountR2DBCMapping.bankAccountFromRow(row, "").also { ba ->
                    ba.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

    fun findAllAssociatedToGroup(groupId: UUID): Flux<BankAccount> {
        val sql =
            """
            SELECT
              ba.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_bank_account gba
            JOIN bank_account ba
              ON ba.id = gba.bank_account_id
            JOIN users u
              ON u.id = ba.user_id
            WHERE
              gba.group_id = :groupId
            ORDER BY ba.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                BankAccountR2DBCMapping.bankAccountFromRow(row, "").also { ba ->
                    ba.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }
}
