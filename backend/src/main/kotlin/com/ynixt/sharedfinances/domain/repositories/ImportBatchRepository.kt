package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ImportBatchRepository : EntityRepository<ImportBatchEntity> {
    fun findFirstByUserIdAndFileHashAndStatusIn(
        userId: UUID,
        fileHash: String,
        statuses: Collection<ImportBatchStatus>,
    ): Mono<ImportBatchEntity>

    fun findAllByUserId(userId: UUID): Flux<ImportBatchEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<ImportBatchEntity>
}
