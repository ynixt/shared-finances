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
    override fun existsExact(
        userId: UUID,
        walletItemId: UUID,
        name: String?,
        value: BigDecimal,
        date: LocalDate,
        installment: Int?,
    ): Mono<Boolean> {
        var spec =
            dbClient
                .sql(
                    """
                    SELECT 1
                    FROM wallet_event we
                    INNER JOIN wallet_entry wen ON wen.wallet_event_id = we.id
                    INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
                    WHERE wi.user_id = :userId
                      AND wen.wallet_item_id = :walletItemId
                      AND we.name IS NOT DISTINCT FROM :name
                      AND wen.value = :value
                      AND we.date = :date
                      AND we.installment IS NOT DISTINCT FROM :installment
                    LIMIT 1
                    """.trimIndent(),
                ).bind("userId", userId)
                .bind("walletItemId", walletItemId)
                .bind("value", value)
                .bind("date", date)

        spec = if (name == null) spec.bindNull("name", String::class.java) else spec.bind("name", name)
        spec = if (installment == null) spec.bindNull("installment", Int::class.java) else spec.bind("installment", installment)

        return spec
            .map { _, _ -> true }
            .one()
            .defaultIfEmpty(false)
    }
}
