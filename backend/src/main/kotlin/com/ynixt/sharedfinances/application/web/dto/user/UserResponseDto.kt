package com.ynixt.sharedfinances.application.web.dto.user

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import java.time.OffsetDateTime
import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val email: String,
    val firstName: String,
    var lastName: String,
    val lang: String,
    val defaultCurrency: String,
    val tmz: String,
    val emailVerified: Boolean,
    val mfaEnabled: Boolean,
    var photoUrl: String?,
    var onboardingDone: Boolean,
    var darkMode: Boolean,
    val role: UserPlanRole,
    val lastLoginAt: OffsetDateTime,
)
