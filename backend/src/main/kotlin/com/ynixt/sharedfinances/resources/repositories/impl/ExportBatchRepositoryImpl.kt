package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.exports.ExportBatchEntity
import com.ynixt.sharedfinances.domain.enums.ExportBatchStatus
import com.ynixt.sharedfinances.domain.repositories.ExportBatchRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.ExportBatchSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class ExportBatchRepositoryImpl(
    springDataRepository: ExportBatchSpringDataRepository,
) : EntityRepositoryImpl<ExportBatchSpringDataRepository, ExportBatchEntity>(springDataRepository),
    ExportBatchRepository {
    override fun findAllByUserId(userId: UUID): Flux<ExportBatchEntity> =
        springDataRepository.findAllByUserIdAndStatusNotOrderByCreatedAtDescIdDesc(userId, ExportBatchStatus.EXPIRED)

    override fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<ExportBatchEntity> = springDataRepository.findByIdAndUserId(id, userId)
}
