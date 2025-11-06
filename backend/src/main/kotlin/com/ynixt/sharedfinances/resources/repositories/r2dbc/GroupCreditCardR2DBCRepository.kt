package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.CreditCardR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class GroupCreditCardR2DBCRepository(
    private val dbClient: DatabaseClient,
) {
    fun findAllAllowedForGroup(groupId: UUID): Flux<CreditCard> {
        val sql =
            """
            SELECT
              ca.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_user gu
            JOIN users u
              ON u.id = gu.user_id
            JOIN credit_card ca
              ON ca.user_id = u.id
            LEFT JOIN group_credit_card gcc
              ON gcc.group_id = gu.group_id
             AND gcc.credit_card_id = ca.id
            WHERE
              gu.group_id = :groupId
              AND gcc.credit_card_id IS NULL
            ORDER BY ca.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                CreditCardR2DBCMapping.creditCardFromRow(row, "").also { ca ->
                    ca.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

    fun findAllAssociatedToGroup(groupId: UUID): Flux<CreditCard> {
        val sql =
            """
            SELECT
              ca.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_credit_card gcc
            JOIN credit_card ca
              ON ca.id = gcc.credit_card_id
            JOIN users u
              ON u.id = ca.user_id
            WHERE
              gcc.group_id = :groupId
            ORDER BY ca.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ ->
                CreditCardR2DBCMapping.creditCardFromRow(row, "").also { ca ->
                    ca.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }
}
