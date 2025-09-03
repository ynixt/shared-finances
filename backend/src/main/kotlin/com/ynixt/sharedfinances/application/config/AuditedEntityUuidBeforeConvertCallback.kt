package com.ynixt.sharedfinances.application.config

import com.fasterxml.uuid.Generators
import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class AuditedEntityUuidBeforeConvertCallback : BeforeConvertCallback<AuditedEntity> {
    override fun onBeforeConvert(
        entity: AuditedEntity,
        table: SqlIdentifier,
    ): Publisher<AuditedEntity> {
        if (entity.id == null) {
            entity.id = Generators.timeBasedEpochRandomGenerator().generate()
        }
        return Mono.just(entity)
    }
}
