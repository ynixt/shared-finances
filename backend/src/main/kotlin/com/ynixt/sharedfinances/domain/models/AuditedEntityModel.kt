package com.ynixt.sharedfinances.domain.models

import java.time.OffsetDateTime

abstract class AuditedEntityModel : SimpleEntityModel() {
    var createdAt: OffsetDateTime? = null
    var updatedAt: OffsetDateTime? = null
}
