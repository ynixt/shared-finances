package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.domain.models.exports.ActiveRecurrenceExportRow
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.springframework.stereotype.Component
import java.io.PipedInputStream
import java.io.PipedOutputStream

@Component
class TransactionXlsxWriter(
    private val recurrenceSheetWriter: RecurrenceXlsxSheetWriter,
) {
    fun write(
        rows: Flow<TransactionExportRow>,
        recurrences: Flow<ActiveRecurrenceExportRow>,
    ): Flow<ByteArray> =
        channelFlow {
            val input = PipedInputStream(BUFFER_SIZE)
            val output = PipedOutputStream(input)
            val writer =
                launch(Dispatchers.IO) {
                    output.use { stream ->
                        Workbook(stream, "Shared Finances", APPLICATION_VERSION).use { workbook ->
                            writeTransactions(workbook.newWorksheet("Transactions"), rows)
                            recurrenceSheetWriter.write(workbook.newWorksheet("Recurrences"), recurrences)
                        }
                    }
                }
            try {
                while (true) {
                    val chunk = withContext(Dispatchers.IO) { input.readNBytes(BUFFER_SIZE) }
                    if (chunk.isEmpty()) break
                    send(chunk)
                }
                writer.join()
            } finally {
                input.close()
            }
        }

    private suspend fun writeTransactions(
        worksheet: Worksheet,
        rows: Flow<TransactionExportRow>,
    ) {
        TransactionCsvWriter.HEADERS.forEachIndexed { column, header -> worksheet.value(0, column, header) }
        worksheet
            .range(0, 0, 0, TransactionCsvWriter.HEADERS.lastIndex)
            .style()
            .bold()
            .set()
        var rowIndex = 1
        rows.collect { row ->
            worksheet.value(rowIndex, 0, row.origin)
            worksheet.value(rowIndex, 1, row.originName)
            worksheet.value(rowIndex, 2, row.date.atStartOfDay())
            worksheet.style(rowIndex, 2).format("yyyy-mm-dd").set()
            worksheet.value(rowIndex, 3, row.description.orEmpty())
            worksheet.value(rowIndex, 4, row.value.toDouble())
            worksheet.style(rowIndex, 4).format("0.00").set()
            worksheet.value(rowIndex, 5, row.currency)
            worksheet.value(rowIndex, 6, row.category.orEmpty())
            worksheet.value(rowIndex, 7, row.categoryName.orEmpty())
            worksheet.value(rowIndex, 8, row.categoryConceptId.orEmpty())
            worksheet.value(rowIndex, 9, row.group.orEmpty())
            worksheet.value(rowIndex, 10, row.groupName.orEmpty())
            worksheet.value(rowIndex, 11, row.installment.orEmpty())
            worksheet.value(rowIndex, 12, row.beneficiaries.orEmpty())
            row.bill?.let {
                worksheet.value(rowIndex, 13, it.atStartOfDay())
                worksheet.style(rowIndex, 13).format("yyyy-mm-dd").set()
            }
            worksheet.value(rowIndex, 14, row.tags.joinToString(","))
            worksheet.value(rowIndex, 15, row.observations.orEmpty())
            worksheet.value(rowIndex, 16, row.confirmed)
            worksheet.value(rowIndex, 17, row.transactionId)
            worksheet.value(rowIndex, 18, row.transferId.orEmpty())
            worksheet.value(rowIndex, 19, row.seriesId.orEmpty())
            rowIndex++
        }
    }

    private companion object {
        const val APPLICATION_VERSION = "3.0"
        const val BUFFER_SIZE = 64 * 1024
    }
}
