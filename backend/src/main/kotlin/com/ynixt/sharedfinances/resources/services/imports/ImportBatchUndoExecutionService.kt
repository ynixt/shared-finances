package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.models.walletentry.DeleteScheduledEntryRequest
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class ImportBatchUndoExecutionService(
    private val importBatchRepository: ImportBatchRepository,
    private val walletEntryRemovalService: WalletEntryRemovalService,
    private val walletEventRepository: WalletEventRepository,
    private val recurrenceEventRepository: RecurrenceEventRepository,
    private val dispatchRepository: ImportBatchDispatchRepository,
    private val clock: Clock,
) {
    @Transactional
    suspend fun execute(
        batchId: UUID,
        workerId: String,
    ) {
        val batch = importBatchRepository.findById(batchId).awaitSingle()
        check(batch.status == ImportBatchStatus.UNDO_RUNNING && batch.workerId == workerId) {
            "Import batch undo is no longer owned by this worker."
        }

        recurrenceEventRepository
            .findAllByImportBatchId(batchId)
            .asFlow()
            .toList()
            .distinctBy { it.seriesId }
            .forEach { config ->
                walletEntryRemovalService.deleteScheduled(
                    userId = batch.userId,
                    recurrenceConfigId = requireNotNull(config.id),
                    request =
                        DeleteScheduledEntryRequest(
                            occurrenceDate = config.nextExecution ?: config.lastExecution ?: LocalDate.now(clock),
                            scope = ScheduledEditScope.ALL_SERIES,
                        ),
                )
            }

        walletEventRepository
            .findAllByImportBatchId(batchId)
            .asFlow()
            .toList()
            .filter { it.recurrenceEventId == null }
            .forEach { event ->
                walletEntryRemovalService.deleteOneOff(
                    userId = batch.userId,
                    walletEventId = requireNotNull(event.id),
                )
            }

        check(dispatchRepository.deleteClaimedUndoBatch(batchId, workerId).awaitSingle() > 0) {
            "Import batch undo lease was lost before completion."
        }
    }
}
