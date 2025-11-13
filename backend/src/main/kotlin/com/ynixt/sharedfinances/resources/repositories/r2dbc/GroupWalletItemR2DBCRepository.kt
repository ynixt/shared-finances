package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.UserR2DBCMapping
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupWalletItemR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository(),
    GroupWalletItemRepository {
    override fun findAllByGroupIdAndEnabled(
        groupId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemSearchResponse> {
        val sort = pageableToSortQuery(pageable, setOf("name"))

        val sql =
            """
            select
                ba.id, ba.name, ba.currency, ${UserR2DBCMapping.createSelectForUser("u")}, 'BANK_ACCOUNT' type
            from group_bank_account gba
            join bank_account ba on ba.id = gba.bank_account_id
            join users u
              on u.id = ba.user_id
            where 
                gba.group_id = :groupId 
                and ba.enabled = :enabled

            union

            select
                cc.id, cc.name, cc.currency, ${UserR2DBCMapping.createSelectForUser("u")}, 'CREDIT_CARD' type
            from group_credit_card gcc
            join credit_card cc on cc.id = gcc.credit_card_id
            join users u
              on u.id = cc.user_id
            where 
                gcc.group_id = :groupId 
                and cc.enabled = :enabled
            $sort
            LIMIT :limit OFFSET :offset
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("enabled", enabled)
            .bind("limit", pageable.pageSize)
            .bind("offset", pageable.offset)
            .map { row, _ ->
                WalletItemSearchResponse(
                    id = row.get("id", UUID::class.java)!!,
                    user = UserR2DBCMapping.userFromRow(row),
                    name = row.get("name", String::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    type = WalletItemType.valueOf(row.get("type", String::class.java)!!),
                )
            }.all()
    }

    override fun countByGroupId(
        groupId: UUID,
        enabled: Boolean,
    ): Mono<Long> {
        val sql =
            """
            SELECT (
                (
                    SELECT COUNT(1) 
                    from group_bank_account gba
                    join bank_account ba on ba.id = gba.bank_account_id
                    WHERE gba.group_id = :groupId AND ba.enabled = :enabled
                )
                +
                (
                    SELECT COUNT(1) 
                    from group_credit_card gcc
                    join credit_card cc on cc.id = gcc.credit_card_id
                    WHERE gcc.group_id = :groupId AND cc.enabled = :enabled
                )
            ) AS total
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .bind("enabled", enabled)
            .map { row, _ -> row.get("total", java.lang.Long::class.java)!!.toLong() }
            .one()
    }
}
