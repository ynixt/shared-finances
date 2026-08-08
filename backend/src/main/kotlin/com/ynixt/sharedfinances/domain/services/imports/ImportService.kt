package com.ynixt.sharedfinances.domain.services.imports

import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportBatchSummary
import com.ynixt.sharedfinances.domain.models.imports.ImportDuplicateCheck
import com.ynixt.sharedfinances.domain.models.imports.ImportHashCheck
import com.ynixt.sharedfinances.domain.models.imports.UndoImportResult
import java.util.UUID

interface ImportService {
    suspend fun checkHash(
        userId: UUID,
        hash: String,
    ): ImportHashCheck

    suspend fun checkDuplicates(
        userId: UUID,
        request: ImportDuplicateCheck,
    ): List<Int>

    suspend fun create(
        userId: UUID,
        request: CreateImport,
    ): ImportBatchSummary

    suspend fun list(userId: UUID): List<ImportBatchSummary>

    suspend fun get(
        userId: UUID,
        batchId: UUID,
    ): ImportBatchSummary?

    suspend fun undo(
        userId: UUID,
        batchId: UUID,
    ): UndoImportResult
}
