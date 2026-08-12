package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.application.config.ExportRetentionProperties
import com.ynixt.sharedfinances.domain.models.exports.ExportPurgeCandidate
import com.ynixt.sharedfinances.domain.services.FileStorageService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ExportPurgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.Resource
import reactor.core.publisher.Flux
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class ExportPurgeServiceTest {
    private val now = Instant.parse("2026-08-11T12:00:00Z")

    @Test
    fun `disabled after-download purge leaves files untouched`() =
        runTest {
            val repository = RecordingPurgeRepository()
            val storage = RecordingStorage()
            val service = service(repository, storage, afterEnabled = false, afterDelay = Duration.ofMinutes(5))

            assertEquals(0, service.purgeAfterDownload())
            assertEquals(null, repository.downloadCutoff)
            assertEquals(emptyList(), storage.deleted)
        }

    @Test
    fun `current delay governs an existing export`() =
        runTest {
            val candidate = ExportPurgeCandidate(UUID.randomUUID(), UUID.randomUUID(), "exports/existing.csv")
            val repository = RecordingPurgeRepository(downloaded = listOf(candidate))
            val storage = RecordingStorage()
            val service = service(repository, storage, afterEnabled = true, afterDelay = Duration.ofHours(1))

            assertEquals(1, service.purgeAfterDownload())
            assertEquals(OffsetDateTime.ofInstant(now.minus(Duration.ofHours(1)), ZoneOffset.UTC), repository.downloadCutoff)
            assertEquals(listOf(candidate.fileKey), storage.deleted)
        }

    @Test
    fun `successful file deletions remove batches and failed deletions remain retryable`() =
        runTest {
            val candidates =
                listOf(
                    ExportPurgeCandidate(UUID.randomUUID(), UUID.randomUUID(), "exports/one.csv"),
                    ExportPurgeCandidate(UUID.randomUUID(), UUID.randomUUID(), "exports/fails.csv"),
                    ExportPurgeCandidate(UUID.randomUUID(), UUID.randomUUID(), "exports/three.csv"),
                )
            val repository = RecordingPurgeRepository(downloaded = candidates)
            val storage = RecordingStorage(failedKeys = setOf("exports/fails.csv"))

            assertEquals(2, service(repository, storage, true, Duration.ofMinutes(5)).purgeAfterDownload())
            assertEquals(1, repository.deleteAllCalls)
            assertEquals(listOf(candidates[0].batchId, candidates[2].batchId), repository.deletedIds)
        }

    private fun service(
        repository: RecordingPurgeRepository,
        storage: RecordingStorage,
        afterEnabled: Boolean,
        afterDelay: Duration,
    ) = ExportPurgeService(
        ExportRetentionProperties(
            afterDownload = ExportRetentionProperties.Policy(afterEnabled, afterDelay, "0 * * * * *"),
            absoluteAge = ExportRetentionProperties.Policy(true, Duration.ofHours(24), "0 * * * * *"),
        ),
        repository,
        storage,
        Mockito.mock(ExportBatchEventPublisher::class.java),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    private class RecordingPurgeRepository(
        private val downloaded: List<ExportPurgeCandidate> = emptyList(),
    ) : ExportPurgeRepository {
        var downloadCutoff: OffsetDateTime? = null
        var deleteAllCalls = 0
        var deletedIds = emptyList<UUID>()

        override fun findDownloadedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate> {
            downloadCutoff = cutoff
            return Flux.fromIterable(downloaded)
        }

        override fun findCompletedBefore(cutoff: OffsetDateTime): Flux<ExportPurgeCandidate> = Flux.empty()

        override fun deleteAll(ids: Collection<UUID>): Flux<UUID> {
            deleteAllCalls++
            deletedIds = ids.toList()
            return Flux.fromIterable(ids)
        }
    }

    private class RecordingStorage(
        private val failedKeys: Set<String> = emptySet(),
    ) : FileStorageService {
        val deleted = mutableListOf<String>()

        override suspend fun write(
            key: String,
            bytes: ByteArray,
        ) = Unit

        override suspend fun write(
            key: String,
            chunks: Flow<ByteArray>,
        ) = Unit

        override suspend fun find(key: String): Resource? = if (key in failedKeys) Mockito.mock(Resource::class.java) else null

        override suspend fun delete(key: String): Boolean = if (key in failedKeys) false else deleted.add(key)
    }
}
