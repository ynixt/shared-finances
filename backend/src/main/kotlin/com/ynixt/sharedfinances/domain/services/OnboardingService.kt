package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import java.util.UUID

interface OnboardingService {
    suspend fun onboarding(
        userId: UUID,
        onboardingDto: UserOnboardingDto,
    ): Boolean
}
