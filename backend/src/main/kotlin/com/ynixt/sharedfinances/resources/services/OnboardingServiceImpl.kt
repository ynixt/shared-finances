package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.OnboardingService
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OnboardingServiceImpl(
    private val userCategoryService: UserCategoryService,
    private val categoryDtoMapper: CategoryDtoMapper,
    private val userRepository: UserRepository,
) : OnboardingService {
    @Transactional
    override suspend fun onboarding(
        userId: UUID,
        onboardingDto: UserOnboardingDto,
    ): Boolean =
        userRepository.changeOnboardingDone(userId, true).awaitSingle().let { modifiedLines ->
            (modifiedLines > 0).also {
                onboardingDto.categories.forEach { cat ->
                    // TODO: use bulk insert
                    userCategoryService
                        .newCategory(
                            userId,
                            categoryDtoMapper.fromNewDtoToNewRequest(cat),
                        )
                }

                userCategoryService.ensureDebtSfCategory(userId)
            }
        }
}
