package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("users")
class UserEntity(
    var email: String,
    var passwordHash: String?,
    var firstName: String,
    var lastName: String,
    var lang: String,
    var defaultCurrency: String,
    var tmz: String,
    var photoUrl: String?,
    var emailVerified: Boolean,
    var mfaEnabled: Boolean,
    var totpSecret: String?,
    var onboardingDone: Boolean,
    var termsAcceptedAt: OffsetDateTime? = null,
    var termsVersion: String? = null,
    var privacyAcceptedAt: OffsetDateTime? = null,
    var privacyVersion: String? = null,
) : AuditedEntity()
