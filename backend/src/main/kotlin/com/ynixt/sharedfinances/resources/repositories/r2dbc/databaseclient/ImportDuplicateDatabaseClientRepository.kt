package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.repositories.ImportDuplicateRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Repository
class ImportDuplicateDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : ImportDuplicateRepository {
    override fun existsExternal(
        userId: UUID,
        walletItemId: UUID,
        externalTransactionId: String,
    ): Mono<Boolean> =
        dbClient
            .sql(
                """
                SELECT 1
                FROM (
                    SELECT we.external_transaction_id
                    FROM wallet_event we
                    INNER JOIN wallet_entry wen ON wen.wallet_event_id = we.id
                    INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                    WHERE wi.user_id = :userId
                      AND wen.wallet_item_id = :walletItemId
                      AND we.external_transaction_id = :externalTransactionId

                    UNION ALL

                    SELECT re.external_transaction_id
                    FROM recurrence_event re
                    INNER JOIN recurrence_entry ren ON ren.wallet_event_id = re.id
                    INNER JOIN wallet_item wi ON wi.id = ren.wallet_item_id
                    WHERE wi.user_id = :userId
                      AND ren.wallet_item_id = :walletItemId
                      AND re.external_transaction_id = :externalTransactionId
                ) matches
                LIMIT 1
                """.trimIndent(),
            ).bind("userId", userId)
            .bind("walletItemId", walletItemId)
            .bind("externalTransactionId", externalTransactionId)
            .map { _, _ -> true }
            .one()
            .defaultIfEmpty(false)

    override fun existsExact(
        userId: UUID,
        walletItemId: UUID,
        name: String?,
        value: BigDecimal,
        date: LocalDate,
        installment: Int?,
        externalTransactionId: String?,
    ): Mono<Boolean> {
        var spec =
            dbClient
                .sql(
                    """
                    SELECT 1
                    FROM wallet_event we
                    INNER JOIN wallet_entry wen ON wen.wallet_event_id = we.id
                    INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                    LEFT JOIN recurrence_event re ON re.id = we.recurrence_event_id
                    WHERE wi.user_id = :userId
                      AND wen.wallet_item_id = :walletItemId
                      AND we.name IS NOT DISTINCT FROM :name
                      AND wen.value = :value
                      AND we.date = :date
                      AND we.installment IS NOT DISTINCT FROM :installment
                      AND (
                          CAST(:externalTransactionId AS VARCHAR) IS NULL
                          OR COALESCE(we.external_transaction_id, re.external_transaction_id) IS NULL
                          OR COALESCE(we.external_transaction_id, re.external_transaction_id) = :externalTransactionId
                      )
                    LIMIT 1
                    """.trimIndent(),
                ).bind("userId", userId)
                .bind("walletItemId", walletItemId)
                .bind("value", value)
                .bind("date", date)

        spec = if (name == null) spec.bindNull("name", String::class.java) else spec.bind("name", name)
        spec = if (installment == null) spec.bindNull("installment", Int::class.java) else spec.bind("installment", installment)
        spec =
            if (externalTransactionId == null) {
                spec.bindNull("externalTransactionId", String::class.java)
            } else {
                spec.bind("externalTransactionId", externalTransactionId)
            }

        return spec
            .map { _, _ -> true }
            .one()
            .defaultIfEmpty(false)
    }
}
