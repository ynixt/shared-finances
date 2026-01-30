package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.OnboardingService
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class OnboardingServiceImpl(
    private val userCategoryService: UserCategoryService,
    private val categoryDtoMapper: CategoryDtoMapper,
    private val userRepository: UserRepository,
) : OnboardingService {
    @Transactional
    override fun onboarding(
        userId: UUID,
        onboardingDto: UserOnboardingDto,
    ): Mono<Void> =
        userRepository.changeOnboardingDone(userId, true).flatMap { modifiedLines ->
            if (modifiedLines == 0) {
                Mono.empty()
            } else {
                Mono
                    .`when`(
                        onboardingDto.categories.map { cat ->
                            // TODO: use bulk insert
                            userCategoryService
                                .newCategory(
                                    userId,
                                    categoryDtoMapper.fromNewDtoToNewRequest(cat),
                                )
                        },
                    ).then()
            }
        }
}
