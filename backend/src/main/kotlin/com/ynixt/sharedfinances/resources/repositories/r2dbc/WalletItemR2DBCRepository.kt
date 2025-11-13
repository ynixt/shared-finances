package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import org.springframework.data.domain.Pageable
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class WalletItemR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository(),
    WalletItemRepository {
    override fun findAllByUserIdAndEnabled(
        userId: UUID,
        enabled: Boolean,
        pageable: Pageable,
    ): Flux<WalletItemSearchResponse> {
        val sort = pageableToSortQuery(pageable, setOf("name"))

        val sql =
            """
            select
                b.id, b.name, b.currency, 'BANK_ACCOUNT' type
            from bank_account b
            where 
                b.user_id = :userId 
                and b.enabled = :enabled

            union

            select
                cc.id, cc.name, cc.currency, 'CREDIT_CARD' type
            from credit_card cc
            where 
                cc.user_id = :userId 
                and cc.enabled = :enabled
            $sort
            LIMIT :limit OFFSET :offset
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("enabled", enabled)
            .bind("limit", pageable.pageSize)
            .bind("offset", pageable.offset)
            .map { row, _ ->
                WalletItemSearchResponse(
                    id = row.get("id", UUID::class.java)!!,
                    user = null,
                    name = row.get("name", String::class.java)!!,
                    currency = row.get("currency", String::class.java)!!,
                    type = WalletItemType.valueOf(row.get("type", String::class.java)!!),
                )
            }.all()
    }

    override fun countByUserId(
        userId: UUID,
        enabled: Boolean,
    ): Mono<Long> {
        val sql =
            """
            SELECT (
                (SELECT COUNT(*) FROM bank_account WHERE user_id = :userId AND enabled = :enabled)
                +
                (SELECT COUNT(*) FROM credit_card WHERE user_id = :userId AND enabled = :enabled)
            ) AS total
            """.trimIndent()

        return dbClient
            .sql(sql)
            .bind("userId", userId)
            .bind("enabled", enabled)
            .map { row, _ -> row.get("total", java.lang.Long::class.java)!!.toLong() }
            .one()
    }
}
