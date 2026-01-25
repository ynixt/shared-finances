package com.ynixt.sharedfinances.application.web.dto.auth

import java.util.UUID

data class LoginResultDto(
    val mfaChallengeId: UUID? = null,
) {
    val mfaRequired = mfaChallengeId != null
}
