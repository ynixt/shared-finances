package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class GroupWalletItemR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository() {
    fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemEntity> {
        val sort = pageableToSortQuery(pageable, setOf("name"))

        val sql =
            """
            select 
                wi.*,
                ${UserR2DBCMapping.createSelectForUser("u")}
            from group_wallet_item gwi
            join wallet_item wi 
                on wi.id = gwi.wallet_item_id
            LEFT JOIN users u
                ON u.id = wi.user_id
            where 
                gwi.group_id = :groupId 
                and wi.enabled = :enabled
            $sort
            LIMIT ${pageable.pageSize} 
            OFFSET ${pageable.offset}
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("enabled", enabled)
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "").also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

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
            JOIN wallet_item wa
              ON wa.user_id = u.id
              AND wa.type = :type
            LEFT JOIN group_wallet_item gwa
              ON gwa.group_id = gu.group_id
              AND gwa.wallet_item_id = wa.id
            WHERE
              gu.group_id = :groupId
              AND gwa.wallet_item_id IS NULL
            ORDER BY wa.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("type", type.toString())
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "").also { wa ->
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
              wa.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM group_wallet_item gwa
            JOIN wallet_item wa
              ON wa.id = gwa.wallet_item_id AND wa.type = :type
            JOIN users u
              ON u.id = wa.user_id
            WHERE
              gwa.group_id = :groupId
            ORDER BY wa.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("type", type.toString())
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "").also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }
}
