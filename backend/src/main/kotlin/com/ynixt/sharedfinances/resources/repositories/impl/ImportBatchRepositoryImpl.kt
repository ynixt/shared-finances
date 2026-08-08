package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.imports.ImportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.ImportBatchSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class ImportBatchRepositoryImpl(
    springDataRepository: ImportBatchSpringDataRepository,
) : EntityRepositoryImpl<ImportBatchSpringDataRepository, ImportBatchEntity>(springDataRepository),
    ImportBatchRepository {
    override fun findFirstByUserIdAndFileHashAndStatusIn(
        userId: UUID,
        fileHash: String,
        statuses: Collection<ImportBatchStatus>,
    ): Mono<ImportBatchEntity> =
        springDataRepository.findFirstByUserIdAndFileHashAndStatusInOrderByCreatedAtDesc(
            userId = userId,
            fileHash = fileHash,
            statuses = statuses,
        )

    override fun findAllByUserId(userId: UUID): Flux<ImportBatchEntity> =
        springDataRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId)

    override fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<ImportBatchEntity> = springDataRepository.findByIdAndUserId(id, userId)
}
