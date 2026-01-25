package com.ynixt.sharedfinances.domain.entities.mfa

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("mfa_recovery_codes")
class MfaRecoveryCodeEntity(
    val userId: UUID,
    val codeHash: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val usedAt: OffsetDateTime? = null,
) : SimpleEntity()
