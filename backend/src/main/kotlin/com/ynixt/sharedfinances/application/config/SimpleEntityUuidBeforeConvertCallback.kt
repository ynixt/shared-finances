package com.ynixt.sharedfinances.application.config

import com.fasterxml.uuid.Generators
import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SimpleEntityUuidBeforeConvertCallback : BeforeConvertCallback<SimpleEntity> {
    override fun onBeforeConvert(
        entity: SimpleEntity,
        table: SqlIdentifier,
    ): Publisher<SimpleEntity> {
        if (entity.id == null) {
            entity.id = Generators.timeBasedEpochRandomGenerator().generate()
        }
        return Mono.just(entity)
    }
}
