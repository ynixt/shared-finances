package com.ynixt.shared_finances.application.config

import com.ynixt.shared_finances.domain.entities.AuditedEntity
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*


@Component
class AuditedEntityUuidBeforeConvertCallback : BeforeConvertCallback<AuditedEntity> {
    override fun onBeforeConvert(entity: AuditedEntity, table: SqlIdentifier): Publisher<AuditedEntity> {
        if (entity.id == null) {
            entity.id = UUID.randomUUID()
        }
        return Mono.just(entity)
    }
}