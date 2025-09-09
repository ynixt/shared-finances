package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.annotation.Id
import java.util.UUID

abstract class SimpleEntity {
    @Id
    var id: UUID? = null
}
