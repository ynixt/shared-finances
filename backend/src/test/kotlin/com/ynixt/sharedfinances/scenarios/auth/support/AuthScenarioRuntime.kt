package com.ynixt.sharedfinances.scenarios.auth.support

import com.ynixt.sharedfinances.application.config.AuthFeatureFlags
import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import com.ynixt.sharedfinances.resources.services.AccountDeletionServiceImpl
import com.ynixt.sharedfinances.resources.services.AuthServiceImpl
import com.ynixt.sharedfinances.resources.services.UserServiceImpl
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.NoOpGroupActionEventServiceStub
import com.ynixt.sharedfinances.scenarios.support.NoOpDatabaseHelperService
import com.ynixt.sharedfinances.scenarios.support.NoOpUserActionEventService
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import kotlinx.coroutines.reactor.awaitSingle
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

internal class AuthScenarioRuntime(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
) {
    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val legalDocumentProperties =
        LegalDocumentProperties(
            termsVersion = "test-terms-1",
            privacyVersion = "test-privacy-1",
        )
    private val authProperties =
        AuthProperties(
            features =
                AuthFeatureFlags(
                    emailConfirmationEnabled = false,
                    passwordRecoveryEnabled = true,
                    turnstileEnabled = false,
                ),
        )

    val infrastructure = AuthScenarioInfrastructure(initialDate)

    private val accountDeletionService =
        AccountDeletionServiceImpl(
            userRepository = infrastructure.userRepository,
            groupRepository = infrastructure.groupStore,
            groupUsersRepository = infrastructure.groupStore,
            groupActionEventService = NoOpGroupActionEventServiceStub,
            groupWalletItemRepository = infrastructure.groupWalletItemRepository,
            walletEventRepository = infrastructure.walletEventRepository,
            recurrenceEventRepository = infrastructure.recurrenceEventRepository,
            simulationJobService = infrastructure.simulationJobService,
            sessionRepository = infrastructure.sessionRepository,
            avatarService = infrastructure.avatarService,
        )

    val userService =
        UserServiceImpl(
            repository = infrastructure.userRepository,
            passwordEncoder = infrastructure.passwordEncoder,
            databaseHelperService = NoOpDatabaseHelperService(),
            avatarService = infrastructure.avatarService,
            legalDocumentProperties = legalDocumentProperties,
            authProperties = authProperties,
            clock = infrastructure.clock,
            accountDeletionService = accountDeletionService,
            userActionEventService = NoOpUserActionEventService(),
        )

    val authService =
        AuthServiceImpl(
            userRepository = infrastructure.userRepository,
            refreshTokenRepository = infrastructure.refreshTokenRepository,
            sessionRepository = infrastructure.sessionRepository,
            failedLoginRepository = infrastructure.failedLoginRepository,
            passwordEncoder = infrastructure.passwordEncoder,
            jwtEncoder = infrastructure.jwtEncoder,
            mfaService = infrastructure.mfaService,
            authProperties = authProperties,
            kid = "scenario-kid",
            issuer = "scenario-shared-finances",
            accessTtlMinutes = 15,
            refreshTtlMinutes = 43200,
            wrongPasswordTtlMinutes = 120,
            wrongPasswordBlock = 10,
        )

    fun validateRegister(request: RegisterDto): Set<ConstraintViolation<RegisterDto>> = validator.validate(request)

    suspend fun loadUserByEmail(email: String) = infrastructure.userRepository.findOneByEmail(email).awaitSingle()

    fun sessionIdFromAccessToken(accessToken: String): UUID {
        val payload =
            accessToken
                .split(".")
                .getOrNull(1)
                ?: error("Invalid access token payload")
        val claims = infrastructure.objectMapper.readValue<Map<String, Any?>>(String(Base64.getUrlDecoder().decode(payload)))
        return UUID.fromString(claims[SESSION_CLAIM_NAME].toString())
    }
}
