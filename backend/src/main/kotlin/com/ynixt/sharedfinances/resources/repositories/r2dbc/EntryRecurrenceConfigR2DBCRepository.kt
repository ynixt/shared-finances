package com.ynixt.sharedfinances.resources.repositories.r2dbc

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.resources.repositories.r2dbc.mapping.EntryRecurrenceConfigR2DBCMapping
import org.springframework.data.domain.Sort
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@Repository
class EntryRecurrenceConfigR2DBCRepository(
    private val dbClient: DatabaseClient,
) : R2BDCGenericRepository() {
    private val validSortColumns =
        mapOf(
            "nextExecution" to "entry_config.next_execution",
            "id" to "entry_config.id",
        )

    fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        originId: UUID?,
        targetId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort = Sort.unsorted(),
    ): Flux<EntryRecurrenceConfigEntity> {
        var sql =
            """
            select entry_config.*
            from entry_recurrence_config entry_config
            where 
            """.trimIndent()

        if (minimumEndExecution != null) { // Abertura
            sql += "(entry_config.end_execution >= :minimumEndExecution OR entry_config.end_execution is null) and "
        }

        if (maximumNextExecution != null) { // Fechamento
            sql += "(entry_config.next_execution <= :maximumNextExecution) and "
        }

        if (originId != null) sql += "entry_config.origin_id = :originId and "
        if (targetId != null) sql += "entry_config.target_id = :targetId and "
        if (userId != null) sql += "entry_config.user_id = :userId and "
        if (groupId != null) sql += "entry_config.group_id = :groupId and "

        sql = sql.removeSuffix("and ")
        sql = sql.removeSuffix("where ")

        val orderClause =
            if (sort.isSorted) {
                sort
                    .stream()
                    .map { order ->
                        val column = validSortColumns[order.property]

                        if (column != null) {
                            val direction = if (order.isAscending) "ASC" else "DESC"
                            "$column $direction"
                        } else {
                            null
                        }
                    }.filter { it != null }
                    .toList()
                    .joinToString(", ")
            } else {
                ""
            }

        if (orderClause.isNotEmpty()) {
            sql += " ORDER BY $orderClause"
        }

        var spec = dbClient.sql(sql)

        if (minimumEndExecution != null) spec = spec.bind("minimumEndExecution", minimumEndExecution)
        if (maximumNextExecution != null) spec = spec.bind("maximumNextExecution", maximumNextExecution)
        if (originId != null) spec = spec.bind("originId", originId)
        if (targetId != null) spec = spec.bind("targetId", targetId)
        if (userId != null) spec = spec.bind("userId", userId)
        if (groupId != null) spec = spec.bind("groupId", groupId)

        return spec
            .map { row, _ ->
                EntryRecurrenceConfigR2DBCMapping.entryRecurrenceConfigFromRow(
                    row,
                    "",
                )
            }.all()
    }
}
