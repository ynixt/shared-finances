package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ImportBatchSpringDataRepository :
    R2dbcRepository<ImportBatchEntity, String>,
    EntityRepository<ImportBatchEntity> {
    fun findFirstByUserIdAndFileHashAndStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
        userId: UUID,
        fileHash: String,
        statuses: Collection<ImportBatchStatus>,
    ): Mono<ImportBatchEntity>

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(userId: UUID): Flux<ImportBatchEntity>

    fun findByIdAndDeletedAtIsNull(id: UUID): Mono<ImportBatchEntity>

    fun findByIdAndUserIdAndDeletedAtIsNull(
        id: UUID,
        userId: UUID,
    ): Mono<ImportBatchEntity>
}
