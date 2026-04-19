package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.WalletItemR2DBCMapping
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupWalletItemDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : DatabaseClientRepository() {
    fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
        walletItemType: WalletItemType? = null,
    ): Flux<WalletItemEntity> {
        val sort = pageableToSortQuery(pageable, setOf("name"))
        val typeClause =
            if (walletItemType == null) {
                ""
            } else {
                " AND wi.type = :walletItemType "
            }

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
                $typeClause
            $sort
            LIMIT ${pageable.pageSize} 
            OFFSET ${pageable.offset}
            """.trimIndent()

        var spec =
            dbClient
                .sql(sql)
                .bind("groupId", groupId)
                .bind("enabled", enabled)
        if (walletItemType != null) {
            spec = spec.bind("walletItemType", walletItemType.toString())
        }
        return spec
            .map { row, _ ->
                WalletItemR2DBCMapping.walletItemFromRow(row, "")!!.also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }

    fun countByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        walletItemType: WalletItemType? = null,
    ): Mono<Long> {
        val typeClause =
            if (walletItemType == null) {
                ""
            } else {
                " AND wi.type = :walletItemType "
            }
        val sql =
            """
            SELECT COUNT(*) AS cnt
            FROM group_wallet_item gwi
            JOIN wallet_item wi ON wi.id = gwi.wallet_item_id
            WHERE gwi.group_id = :groupId AND wi.enabled = :enabled
            $typeClause
            """.trimIndent()
        var spec =
            dbClient
                .sql(sql)
                .bind("groupId", groupId)
                .bind("enabled", enabled)
        if (walletItemType != null) {
            spec = spec.bind("walletItemType", walletItemType.toString())
        }
        return spec
            .map { row, _ -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
    }

    fun findAllAllowedForGroup(
        userId: UUID,
        groupId: UUID,
        type: WalletItemType,
    ): Flux<WalletItemEntity> {
        val sql =
            """
            SELECT
              wa.*,
              ${UserR2DBCMapping.createSelectForUser("u")}
            FROM wallet_item wa
            JOIN users u
              ON u.id = wa.user_id
            LEFT JOIN group_wallet_item gwa
              ON gwa.group_id = :groupId
              AND gwa.wallet_item_id = wa.id
            WHERE
              wa.user_id = :userId
              AND wa.type = :type
              AND gwa.wallet_item_id IS NULL
            ORDER BY wa.name
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("groupId", groupId)
            .bind("type", type.toString())
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
                WalletItemR2DBCMapping.walletItemFromRow(row, "")!!.also { wa ->
                    wa.user = UserR2DBCMapping.userFromRow(row)
                }
            }.all()
    }
}
