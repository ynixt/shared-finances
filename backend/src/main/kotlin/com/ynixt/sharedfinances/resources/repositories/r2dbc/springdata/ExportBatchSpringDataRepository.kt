package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ExportBatchSpringDataRepository :
    R2dbcRepository<ExportBatchEntity, String>,
    EntityRepository<ExportBatchEntity> {
    fun findAllByUserIdAndStatusNotOrderByCreatedAtDescIdDesc(
        userId: UUID,
        status: ExportBatchStatus,
    ): Flux<ExportBatchEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<ExportBatchEntity>
}
