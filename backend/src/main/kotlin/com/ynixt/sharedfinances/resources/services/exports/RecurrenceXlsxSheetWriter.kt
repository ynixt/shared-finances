package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.domain.models.exports.ActiveRecurrenceExportRow
import kotlinx.coroutines.flow.Flow
import org.dhatim.fastexcel.Worksheet
import org.springframework.stereotype.Component

@Component
class RecurrenceXlsxSheetWriter {
    suspend fun write(
        worksheet: Worksheet,
        recurrences: Flow<ActiveRecurrenceExportRow>,
    ) {
        HEADERS.forEachIndexed { column, header -> worksheet.value(0, column, header) }
        worksheet
            .range(0, 0, 0, HEADERS.lastIndex)
            .style()
            .bold()
            .set()
        var rowIndex = 1
        recurrences.collect { recurrence ->
            worksheet.value(rowIndex, 0, recurrence.description.orEmpty())
            worksheet.value(rowIndex, 1, recurrence.paymentType.name)
            recurrence.nextExecution?.let {
                worksheet.value(rowIndex, 2, it.atStartOfDay())
                worksheet.style(rowIndex, 2).format("yyyy-mm-dd").set()
            }
            worksheet.value(rowIndex, 3, recurrence.category.orEmpty())
            worksheet.value(rowIndex, 4, recurrence.group.orEmpty())
            worksheet.value(rowIndex, 5, recurrence.seriesId.toString())
            rowIndex++
        }
    }

    private companion object {
        val HEADERS = listOf("description", "payment type", "next execution", "category", "group", "series id")
    }
}
