package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryConceptException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryException
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.OnboardingService
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.categories.UserCategoryService
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OnboardingServiceImpl(
    private val userCategoryService: UserCategoryService,
    private val categoryDtoMapper: CategoryDtoMapper,
    private val userRepository: UserRepository,
    private val actionEventService: ActionEventService,
) : OnboardingService {
    private val logger = LoggerFactory.getLogger(OnboardingServiceImpl::class.java)

    @Transactional
    override suspend fun onboarding(
        userId: UUID,
        onboardingDto: UserOnboardingDto,
    ): Boolean =
        userRepository.changeOnboardingDone(userId, true).awaitSingle().let { modifiedLines ->
            val onboardingDoneNow = modifiedLines > 0

            if (!onboardingDoneNow) return false

            onboardingDto.categories.forEach { cat ->
                try {
                    userCategoryService
                        .newCategory(
                            userId,
                            categoryDtoMapper.fromNewDtoToNewRequest(cat),
                        )
                } catch (_: DuplicatedCategoryConceptException) {
                    // Duplicated request should be ignored
                } catch (_: DuplicatedCategoryException) {
                    // Duplicated request should be ignored
                }
            }

            try {
                userCategoryService.ensureDebtSfCategory(userId)
            } catch (_: DuplicatedCategoryException) {
                // Duplicated request should be ignored
            }

            runCatching {
                actionEventService.newEvent(
                    userId = userId,
                    type = ActionEventType.UPDATE,
                    category = ActionEventCategory.ONBOARDING,
                    data = true,
                )
            }.onFailure { error ->
                logger.warn("Failed to dispatch onboarding event for user {}", userId, error)
            }

            return true
        }
}
