package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import reactor.core.publisher.Mono
import java.util.UUID

interface OnboardingService {
    fun onboarding(
        userId: UUID,
        onboardingDto: UserOnboardingDto,
    ): Mono<Void>
}
