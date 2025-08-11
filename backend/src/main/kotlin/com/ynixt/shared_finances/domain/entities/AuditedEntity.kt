package com.ynixt.shared_finances.domain.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import java.time.OffsetDateTime
import java.util.*

abstract class AuditedEntity {
    @Id
    var id: UUID? = null

    @CreatedDate
    var createdAt: OffsetDateTime? = null

    @LastModifiedDate
    var updatedAt: OffsetDateTime? = null
}