package com.ynixt.sharedfinances.domain.entities.mfa

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("mfa_enrollments")
class MfaEnrollmentEntity(
    val userId: UUID,
    val secretEnc: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val expiresAt: OffsetDateTime = OffsetDateTime.now().plusMinutes(10),
) : SimpleEntity()
