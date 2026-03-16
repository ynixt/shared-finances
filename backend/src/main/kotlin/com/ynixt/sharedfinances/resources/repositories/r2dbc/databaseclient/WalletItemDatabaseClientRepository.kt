package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class WalletItemDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    fun findAllAllowedForGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> {
        val sql =
            """
            SELECT
              wa.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_user gu
            JOIN users u
              ON u.id = gu.user_id
            JOIN bank_account wa
              ON wa.user_id = u.id
            LEFT JOIN group_wallet_item gwa
              ON gwa.group_id = gu.group_id AND type = :type
             AND gwa.bank_account_id = wa.id
            WHERE
              gu.group_id = :groupId
              AND gwa.bank_account_id IS NULL
            ORDER BY wa.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("type", type)
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "")!!.also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

    fun findAllAssociatedToGroup(
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> {
        val sql =
            """
            SELECT
              walletItemFromRow.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_bank_account gwa
            JOIN group_wallet_item wa
              ON wa.id = gwa.bank_account_id AND type = :type
            JOIN users u
              ON u.id = wa.user_id
            WHERE
              gwa.group_id = :groupId
            ORDER BY wa.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("type", type)
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "")!!.also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }
}
