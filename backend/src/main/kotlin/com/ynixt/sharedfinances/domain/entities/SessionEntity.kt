package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table
import java.net.InetAddress
import java.util.UUID

@Table("session")
class SessionEntity(
    val userId: UUID,
    val userAgent: String?,
    val ip: InetAddress?,
) : AuditedEntity()
