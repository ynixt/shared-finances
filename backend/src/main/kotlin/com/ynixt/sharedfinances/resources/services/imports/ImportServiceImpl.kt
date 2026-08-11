package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ImportHashStatus
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidImportRequestException
import com.ynixt.sharedfinances.domain.exceptions.http.WalletItemNotFoundException
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportBatchSummary
import com.ynixt.sharedfinances.domain.models.imports.ImportDuplicateCheck
import com.ynixt.sharedfinances.domain.models.imports.ImportHashCheck
import com.ynixt.sharedfinances.domain.models.imports.UndoImportResult
import com.ynixt.sharedfinances.domain.queue.producer.ImportJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.ImportDuplicateRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.imports.ImportService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ImportServiceImpl(
    private val importBatchRepository: ImportBatchRepository,
    private val duplicateRepository: ImportDuplicateRepository,
    private val walletItemService: WalletItemService,
    private val acceptanceService: ImportBatchAcceptanceService,
    private val dispatchRepository: ImportBatchDispatchRepository,
    private val dispatchQueueProducer: ImportJobDispatchQueueProducer,
    private val eventPublisher: ImportBatchEventPublisher,
    private val planQuotaService: PlanQuotaService,
) : ImportService {
    private val logger = LoggerFactory.getLogger(ImportServiceImpl::class.java)

    override suspend fun checkHash(
        userId: UUID,
        hash: String,
    ): ImportHashCheck {
        val normalizedHash = normalizeHash(hash)
        val active =
            importBatchRepository
                .findFirstByUserIdAndFileHashAndStatusIn(userId, normalizedHash, ACTIVE_STATUSES)
                .awaitSingleOrNull()
        if (active != null) {
            return ImportHashCheck(
                status = ImportHashStatus.PROCESSING,
                batchId = active.id,
                importedAt = active.createdAt,
                fileName = active.fileName,
            )
        }

        val completed =
            importBatchRepository
                .findFirstByUserIdAndFileHashAndStatusIn(userId, normalizedHash, IMPORTED_STATUSES)
                .awaitSingleOrNull()
        return if (completed == null) {
            ImportHashCheck(status = ImportHashStatus.NOT_IMPORTED)
        } else {
            ImportHashCheck(
                status = ImportHashStatus.IMPORTED,
                batchId = completed.id,
                importedAt = completed.finishedAt ?: completed.createdAt,
                fileName = completed.fileName,
            )
        }
    }

    override suspend fun checkDuplicates(
        userId: UUID,
        request: ImportDuplicateCheck,
    ): List<Int> {
        request.lines
            .map { it.walletItemId }
            .distinct()
            .forEach { requireOwnedWalletItem(userId, it) }
        return request.lines.mapIndexedNotNull { index, line ->
            val externalTransactionId = normalizeExternalTransactionId(line.externalTransactionId)
            val duplicateByExternalId =
                externalTransactionId?.let {
                    duplicateRepository
                        .existsExternal(
                            userId = userId,
                            walletItemId = line.walletItemId,
                            externalTransactionId = it,
                        ).awaitSingle()
                } ?: false
            val duplicate =
                duplicateByExternalId ||
                    duplicateRepository
                        .existsExact(
                            userId = userId,
                            walletItemId = line.walletItemId,
                            name = line.name?.trim()?.ifBlank { null },
                            value = line.value,
                            date = line.date,
                            installment = line.installment,
                            externalTransactionId = externalTransactionId,
                        ).awaitSingle()
            duplicate
                .takeIf { it }
                ?.let { index }
        }
    }

    @Transactional
    override suspend fun create(
        userId: UUID,
        request: CreateImport,
    ): ImportBatchSummary {
        planQuotaService.assertCanAdd(userId, PlanLimitKey.IMPORTS_PER_MONTH)
        val accepted =
            try {
                acceptanceService.accept(userId, request)
            } catch (error: DataIntegrityViolationException) {
                acceptanceService.findActive(userId, request.fileHash) ?: throw error
            }

        val batchId = requireNotNull(accepted.batch.id) { "import batch id" }
        if (accepted.created) {
            eventPublisher.publish(accepted.batch, ActionEventType.INSERT)
        }
        dispatchSafely(batchId)
        return accepted.batch.toSummary(accepted.walletItemName)
    }

    override suspend fun list(userId: UUID): List<ImportBatchSummary> {
        val batches = importBatchRepository.findAllByUserId(userId).asFlow().toList()
        val walletItems =
            walletItemService
                .findAllByIdIn(batches.mapNotNull { it.walletItemId }.distinct())
                .toList()
                .associateBy { requireNotNull(it.id) }
        return batches.map { batch ->
            batch.toSummary(
                batch.walletItemId?.let { walletItems[it]?.name ?: REMOVED_ACCOUNT_LABEL } ?: MULTIPLE_ORIGINS_LABEL,
            )
        }
    }

    override suspend fun get(
        userId: UUID,
        batchId: UUID,
    ): ImportBatchSummary? {
        val batch = importBatchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull() ?: return null
        val walletItemName =
            batch.walletItemId?.let { walletItemService.findOne(it)?.name ?: REMOVED_ACCOUNT_LABEL } ?: MULTIPLE_ORIGINS_LABEL
        return batch.toSummary(walletItemName)
    }

    override suspend fun undo(
        userId: UUID,
        batchId: UUID,
    ): UndoImportResult {
        val initial = importBatchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull() ?: return UndoImportResult.NotFound
        val accepted =
            when (initial.status) {
                ImportBatchStatus.UNDO_QUEUED,
                ImportBatchStatus.UNDO_RUNNING,
                -> initial

                ImportBatchStatus.COMPLETED,
                ImportBatchStatus.UNDO_FAILED,
                -> {
                    val transitioned =
                        try {
                            dispatchRepository.queueUndo(batchId, userId).awaitSingleOrNull()
                        } catch (_: DataIntegrityViolationException) {
                            return UndoImportResult.InvalidStatus
                        }
                    val current =
                        importBatchRepository.findByIdAndUserId(batchId, userId).awaitSingleOrNull()
                            ?: return UndoImportResult.NotFound
                    if (current.status !in UNDO_ACTIVE_STATUSES) {
                        return UndoImportResult.InvalidStatus
                    }
                    if (transitioned != null) {
                        eventPublisher.publish(current, ActionEventType.UPDATE)
                    }
                    current
                }

                else -> return UndoImportResult.InvalidStatus
            }

        dispatchSafely(batchId)
        return UndoImportResult.Accepted(accepted.toSummary(resolveWalletItemName(accepted)))
    }

    private suspend fun requireOwnedWalletItem(
        userId: UUID,
        walletItemId: UUID,
    ) = walletItemService
        .findOne(walletItemId)
        ?.takeIf { it.userId == userId }
        ?: throw WalletItemNotFoundException(walletItemId)

    private fun dispatchSafely(batchId: UUID) {
        runCatching { dispatchQueueProducer.send(batchId) }
            .onFailure { error -> logger.error("Failed to publish import batch dispatch for $batchId", error) }
    }

    private fun normalizeHash(hash: String): String {
        val normalized = hash.trim().lowercase()
        require(normalized.matches(Regex("[0-9a-f]{64}"))) { "fileHash must be a SHA-256 hexadecimal hash." }
        return normalized
    }

    private fun normalizeExternalTransactionId(value: String?): String? =
        value
            ?.trim()
            ?.ifBlank { null }
            ?.also {
                if (it.length > MAX_EXTERNAL_TRANSACTION_ID_LENGTH) {
                    throw InvalidImportRequestException(
                        "externalTransactionId must have at most $MAX_EXTERNAL_TRANSACTION_ID_LENGTH characters.",
                    )
                }
            }

    private suspend fun resolveWalletItemName(batch: ImportBatchEntity): String =
        batch.walletItemId?.let { walletItemService.findOne(it)?.name ?: REMOVED_ACCOUNT_LABEL } ?: MULTIPLE_ORIGINS_LABEL

    private fun ImportBatchEntity.toSummary(walletItemName: String): ImportBatchSummary =
        ImportBatchSummary(
            id = requireNotNull(id),
            fileHash = fileHash,
            fileName = fileName,
            format = format,
            walletItemId = walletItemId,
            walletItemName = walletItemName,
            qty = qty,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            status = status,
            errorMessage = errorMessage,
            createdAt = requireNotNull(createdAt),
            startedAt = startedAt,
            finishedAt = finishedAt,
            retries = retries,
        )

    private companion object {
        val ACTIVE_STATUSES =
            setOf(
                ImportBatchStatus.QUEUED,
                ImportBatchStatus.RUNNING,
                ImportBatchStatus.UNDO_QUEUED,
                ImportBatchStatus.UNDO_RUNNING,
            )
        val UNDO_ACTIVE_STATUSES = setOf(ImportBatchStatus.UNDO_QUEUED, ImportBatchStatus.UNDO_RUNNING)
        val IMPORTED_STATUSES = setOf(ImportBatchStatus.COMPLETED, ImportBatchStatus.UNDO_FAILED)
        const val MULTIPLE_ORIGINS_LABEL = "Múltiplas origens"
        const val REMOVED_ACCOUNT_LABEL = "Conta removida"
        const val MAX_EXTERNAL_TRANSACTION_ID_LENGTH = 255
    }
}
