package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.exceptions.http.WalletItemNotFoundException
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportLine
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.services.WalletItemService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.UUID

data class AcceptedImportBatch(
    val batch: ImportBatchEntity,
    val walletItemName: String,
    val created: Boolean,
)

@Service
class ImportBatchAcceptanceService(
    private val importBatchRepository: ImportBatchRepository,
    private val walletItemService: WalletItemService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    suspend fun accept(
        userId: UUID,
        request: CreateImport,
    ): AcceptedImportBatch {
        require(request.lines.isNotEmpty()) { "At least one import line is required." }
        val normalized = normalize(request)
        val active = findActiveEntity(userId, normalized.fileHash)
        if (active != null) {
            return AcceptedImportBatch(active, resolveWalletItemName(active), created = false)
        }

        val walletItems =
            normalized.lines
                .map { it.walletItemId }
                .distinct()
                .associateWith { requireOwnedWalletItem(userId, it) }
        val quantities = normalized.lines.sumOf(::quantityCreated)
        val totalCredit =
            normalized.lines
                .filter { it.value.signum() >= 0 }
                .fold(BigDecimal.ZERO) { total, line ->
                    total.add(line.value.abs().multiply(quantityCreated(line).toBigDecimal()))
                }
        val totalDebit =
            normalized.lines
                .filter { it.value.signum() < 0 }
                .fold(BigDecimal.ZERO) { total, line ->
                    total.add(line.value.abs().multiply(quantityCreated(line).toBigDecimal()))
                }

        val saved =
            importBatchRepository
                .save(
                    ImportBatchEntity(
                        userId = userId,
                        fileHash = normalized.fileHash,
                        fileName = normalized.fileName,
                        format = normalized.format,
                        walletItemId = walletItems.keys.singleOrNull(),
                        qty = quantities,
                        totalCredit = totalCredit,
                        totalDebit = totalDebit,
                        status = ImportBatchStatus.QUEUED,
                        requestPayload = objectMapper.writeValueAsString(normalized),
                        errorMessage = null,
                        leaseExpiresAt = null,
                        workerId = null,
                        startedAt = null,
                        finishedAt = null,
                        retries = 0,
                    ),
                ).awaitSingle()

        val persisted = importBatchRepository.findById(requireNotNull(saved.id) { "import batch id" }).awaitSingle()
        return AcceptedImportBatch(
            batch = persisted,
            walletItemName = walletItems.values.singleOrNull()?.name ?: MULTIPLE_ORIGINS_LABEL,
            created = true,
        )
    }

    suspend fun findActive(
        userId: UUID,
        fileHash: String,
    ): AcceptedImportBatch? =
        findActiveEntity(userId, normalizeHash(fileHash))?.let { active ->
            AcceptedImportBatch(active, resolveWalletItemName(active), created = false)
        }

    private suspend fun findActiveEntity(
        userId: UUID,
        fileHash: String,
    ): ImportBatchEntity? =
        importBatchRepository
            .findFirstByUserIdAndFileHashAndStatusIn(userId, fileHash, ACTIVE_STATUSES)
            .awaitSingleOrNull()

    private suspend fun resolveWalletItemName(batch: ImportBatchEntity): String =
        batch.walletItemId?.let { walletItemService.findOne(it)?.name ?: REMOVED_ACCOUNT_LABEL } ?: MULTIPLE_ORIGINS_LABEL

    private suspend fun requireOwnedWalletItem(
        userId: UUID,
        walletItemId: UUID,
    ) = walletItemService
        .findOne(walletItemId)
        ?.takeIf { it.userId == userId }
        ?: throw WalletItemNotFoundException(walletItemId)

    private fun normalize(request: CreateImport): CreateImport =
        request.copy(
            fileHash = normalizeHash(request.fileHash),
            fileName = request.fileName.trim().take(255),
            format =
                request.format
                    .trim()
                    .uppercase()
                    .take(16),
            lines =
                request.lines.map { line ->
                    validateInstallment(line)
                    line.copy(
                        name = line.name?.trim()?.ifBlank { null },
                        tags =
                            line.tags
                                ?.map(String::trim)
                                ?.filter(String::isNotBlank)
                                ?.distinct(),
                        observations = line.observations?.trim()?.ifBlank { null },
                    )
                },
        )

    private fun quantityCreated(line: ImportLine): Int {
        validateInstallment(line)
        val number = line.installment ?: return 1
        val total = requireNotNull(line.installmentTotal)
        return 1 +
            (if (line.createPreviousInstallments) number - 1 else 0) +
            (if (line.createFollowingInstallments) total - number else 0)
    }

    private fun validateInstallment(line: ImportLine) {
        val number = line.installment
        val total = line.installmentTotal
        require((number == null) == (total == null)) { "Installment number and total must be provided together." }
        if (number != null && total != null) {
            require(number in 1..total) { "Installment number must be between 1 and the installment total." }
        }
    }

    private fun normalizeHash(hash: String): String {
        val normalized = hash.trim().lowercase()
        require(normalized.matches(Regex("[0-9a-f]{64}"))) { "fileHash must be a SHA-256 hexadecimal hash." }
        return normalized
    }

    private companion object {
        val ACTIVE_STATUSES =
            setOf(
                ImportBatchStatus.QUEUED,
                ImportBatchStatus.RUNNING,
                ImportBatchStatus.UNDO_QUEUED,
                ImportBatchStatus.UNDO_RUNNING,
            )
        const val MULTIPLE_ORIGINS_LABEL = "Múltiplas origens"
        const val REMOVED_ACCOUNT_LABEL = "Conta removida"
    }
}
