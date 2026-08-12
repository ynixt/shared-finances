package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.models.exports.ActiveRecurrenceExportRow
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TransactionExportRepository {
    suspend fun countLines(
        userId: UUID,
        filter: TransactionExportFilter,
    ): Long

    suspend fun findRows(
        userId: UUID,
        filter: TransactionExportFilter,
        pageSize: Int = 500,
    ): Flow<TransactionExportRow>

    suspend fun findActiveRecurrences(
        userId: UUID,
        filter: TransactionExportFilter,
    ): Flow<ActiveRecurrenceExportRow>
}
