package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.config.ExportRetentionProperties
import com.ynixt.sharedfinances.domain.models.exports.ExportPurgeCandidate
import com.ynixt.sharedfinances.domain.services.FileStorageService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportPurgeRepository
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ExportPurgeService(
    private val properties: ExportRetentionProperties,
    private val repository: ExportPurgeRepository,
    private val storageService: FileStorageService,
    private val eventPublisher: ExportBatchEventPublisher,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun purgeAfterDownload(): Int {
        if (!properties.afterDownload.enabled) return 0
        val cutoff = OffsetDateTime.now(clock).minus(properties.afterDownload.delay)
        return purge(repository.findDownloadedBefore(cutoff).asFlow())
    }

    suspend fun purgeByAbsoluteAge(): Int {
        if (!properties.absoluteAge.enabled) return 0
        val cutoff = OffsetDateTime.now(clock).minus(properties.absoluteAge.delay)
        return purge(repository.findCompletedBefore(cutoff).asFlow())
    }

    private suspend fun purge(candidates: kotlinx.coroutines.flow.Flow<ExportPurgeCandidate>): Int {
        val deletedCandidates = mutableMapOf<UUID, ExportPurgeCandidate>()
        candidates.collect { candidate ->
            if (deleteFile(candidate)) deletedCandidates[candidate.batchId] = candidate
        }
        if (deletedCandidates.isEmpty()) return 0
        val deletedIds = repository.deleteAll(deletedCandidates.keys).collectList().awaitSingle()
        deletedIds.forEach { id ->
            deletedCandidates[id]?.let { eventPublisher.publishDeleted(it.userId, id) }
        }
        return deletedIds.size
    }

    private suspend fun deleteFile(candidate: ExportPurgeCandidate): Boolean =
        try {
            val removed = storageService.delete(candidate.fileKey)
            if (!removed && storageService.find(candidate.fileKey) != null) {
                logger.warn("Export file was not deleted; batch {} will be retried", candidate.batchId)
                false
            } else {
                true
            }
        } catch (error: Exception) {
            logger.warn("Failed to delete export file for batch {}; it will be retried", candidate.batchId, error)
            false
        }
}
