package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table

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
) : AuditedEntity()
