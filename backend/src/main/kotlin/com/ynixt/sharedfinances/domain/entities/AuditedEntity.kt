package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import java.time.OffsetDateTime
import java.util.UUID

abstract class AuditedEntity {
    @Id
    var id: UUID? = null

    @CreatedDate
    var createdAt: OffsetDateTime? = null

    @LastModifiedDate
    var updatedAt: OffsetDateTime? = null
}
