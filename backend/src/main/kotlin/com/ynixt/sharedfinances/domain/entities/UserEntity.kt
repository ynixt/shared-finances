package com.ynixt.sharedfinances.domain.entities

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
    var darkMode: Boolean = false,
    var termsAcceptedAt: OffsetDateTime? = null,
    var termsVersion: String? = null,
    var privacyAcceptedAt: OffsetDateTime? = null,
    var privacyVersion: String? = null,
    var role: UserPlanRole = UserPlanRole.PRO,
    var lastLoginAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    var inactivityNoticeStage: Int? = null,
) : AuditedEntity()
