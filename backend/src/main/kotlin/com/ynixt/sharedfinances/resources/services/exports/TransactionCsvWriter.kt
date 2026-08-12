package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class TransactionCsvWriter {
    fun write(rows: Flow<TransactionExportRow>): Flow<ByteArray> =
        flow {
            emit(("\uFEFF" + HEADERS.joinToString(";") + "\r\n").toByteArray(StandardCharsets.UTF_8))
            rows.collect { row ->
                val cells =
                    listOf(
                        row.origin,
                        row.originName,
                        row.date.toString(),
                        row.description,
                        row.value.toPlainString(),
                        row.currency,
                        row.category,
                        row.categoryName,
                        row.categoryConceptId,
                        row.group,
                        row.groupName,
                        row.installment,
                        row.beneficiaries,
                        row.bill?.toString(),
                        row.tags.joinToString(","),
                        row.observations,
                        row.confirmed.toString(),
                        row.transactionId,
                        row.transferId,
                        row.seriesId,
                    )
                emit((cells.joinToString(";") { escapeCell(it.orEmpty()) } + "\r\n").toByteArray(StandardCharsets.UTF_8))
            }
        }

    internal fun escapeCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (value.any { it == ';' || it == '"' || it == '\r' || it == '\n' }) "\"$escaped\"" else escaped
    }

    companion object {
        val HEADERS =
            listOf(
                "origin",
                "origin_name",
                "date",
                "description",
                "value",
                "currency",
                "category",
                "category_name",
                "category_concept_id",
                "group",
                "group_name",
                "installment",
                "beneficiaries",
                "bill",
                "tags",
                "observations",
                "confirmed",
                "transaction id",
                "transfer id",
                "series id",
            )
    }
}
