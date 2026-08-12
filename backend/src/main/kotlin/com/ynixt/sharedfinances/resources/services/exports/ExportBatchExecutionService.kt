package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.web.validation.ExportLineLimitValidator
import com.ynixt.sharedfinances.domain.enums.ExportFormat
import com.ynixt.sharedfinances.domain.exceptions.http.ExportSelectionEmptyException
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.TransactionExportRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.FileStorageService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportBatchDispatchRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ExportBatchExecutionService(
    private val batchRepository: ExportBatchRepository,
    private val transactionExportRepository: TransactionExportRepository,
    private val userRepository: UserRepository,
    private val lineLimitValidator: ExportLineLimitValidator,
    private val csvWriter: TransactionCsvWriter,
    private val xlsxWriter: TransactionXlsxWriter,
    private val storageService: FileStorageService,
    private val dispatchRepository: ExportBatchDispatchRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    suspend fun execute(
        batchId: UUID,
        workerId: String,
    ) {
        val batch = batchRepository.findById(batchId).awaitSingle()
        val filter = objectMapper.readValue(batch.filterPayload, TransactionExportFilter::class.java)
        val count = transactionExportRepository.countLines(batch.userId, filter)
        if (count == 0L) throw ExportSelectionEmptyException()
        val role = userRepository.findById(batch.userId).awaitSingle().role
        lineLimitValidator.validate(role, count)
        val fileKey = "exports/${batch.userId}/$batchId.${batch.format.name.lowercase()}"
        val rows = transactionExportRepository.findRows(batch.userId, filter)
        val content =
            when (batch.format) {
                ExportFormat.CSV -> csvWriter.write(rows)
                ExportFormat.XLSX -> xlsxWriter.write(rows, transactionExportRepository.findActiveRecurrences(batch.userId, filter))
            }
        try {
            storageService.write(fileKey, content)
            check(
                dispatchRepository
                    .markCompleted(
                        batchId = batchId,
                        workerId = workerId,
                        rowCount = count.toInt(),
                        fileKey = fileKey,
                        finishedAt = OffsetDateTime.now(clock),
                    ).awaitSingle() == 1L,
            ) { "Export lease was lost before completion." }
        } catch (error: Exception) {
            storageService.delete(fileKey)
            throw error
        }
    }
}
