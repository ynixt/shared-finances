package com.ynixt.sharedfinances.domain.entities

import java.net.InetAddress
import java.util.UUID

class SessionEntity(
    val userId: UUID,
    val userAgent: String?,
    val ip: InetAddress?,
) : AuditedEntity()
