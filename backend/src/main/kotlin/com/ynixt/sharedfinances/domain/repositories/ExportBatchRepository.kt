package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ExportBatchRepository : EntityRepository<ExportBatchEntity> {
    fun findAllByUserId(userId: UUID): Flux<ExportBatchEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<ExportBatchEntity>
}
