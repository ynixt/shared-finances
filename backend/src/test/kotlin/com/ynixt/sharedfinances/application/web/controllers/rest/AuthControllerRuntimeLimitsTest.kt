package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.config.AuthFeatureFlags
import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.application.web.dto.auth.LoginDto
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.exceptions.http.auth.RegistrationDisabledException
import com.ynixt.sharedfinances.domain.exceptions.http.auth.RegistrationLegalAcceptanceRequiredException
import com.ynixt.sharedfinances.domain.models.LoginResult
import com.ynixt.sharedfinances.domain.services.AuthService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.auth.OpenAuthEmailWorkflowService
import com.ynixt.sharedfinances.domain.services.captcha.CaptchaService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthControllerRuntimeLimitsTest {
    private val userService = Mockito.mock(UserService::class.java)
    private val authService = Mockito.mock(AuthService::class.java)
    private val captchaService = Mockito.mock(CaptchaService::class.java)
    private val emailWorkflowService = Mockito.mock(OpenAuthEmailWorkflowService::class.java)

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(userService, authService, captchaService, emailWorkflowService)
    }

    @Test
    fun `reports disabled registration and rejects it before side effects`() =
        runTest {
            val controller = controller(registrationEnabled = false)

            assertEquals(false, controller.openAuthPreferences().body!!.registrationEnabled)
            assertFailsWith<RegistrationDisabledException> {
                controller.register(Mockito.mock(ServerWebExchange::class.java), validRegisterRequest())
            }

            Mockito.verifyNoInteractions(userService, captchaService, emailWorkflowService)
        }

    @Test
    fun `reports enabled registration by default`() =
        runTest {
            assertEquals(true, controller(registrationEnabled = true).openAuthPreferences().body!!.registrationEnabled)
        }

    @Test
    fun `reports both public instance switches in either position`() =
        runTest {
            val disabled = controller(legalDocumentsEnabled = false, planLimitsEnabled = false).openAuthPreferences().body!!
            val enabled = controller(legalDocumentsEnabled = true, planLimitsEnabled = true).openAuthPreferences().body!!

            assertEquals(false, disabled.legalDocumentsEnabled)
            assertEquals(false, disabled.planLimitsEnabled)
            assertEquals(true, enabled.legalDocumentsEnabled)
            assertEquals(true, enabled.planLimitsEnabled)
        }

    @Test
    fun `requires legal acceptance only while documents are presented`() =
        runTest {
            val request = validRegisterRequest().copy(acceptTerms = false, acceptPrivacy = false)

            assertFailsWith<RegistrationLegalAcceptanceRequiredException> {
                controller(legalDocumentsEnabled = true).register(Mockito.mock(ServerWebExchange::class.java), request)
            }
            Mockito.verifyNoInteractions(userService, captchaService, emailWorkflowService)
        }

    @Test
    fun `allows registration without legal acceptance while documents are absent`() =
        runTest {
            val request = validRegisterRequest().copy(acceptTerms = false, acceptPrivacy = false)
            val exchange = Mockito.mock(ServerWebExchange::class.java)
            val serverRequest = Mockito.mock(ServerHttpRequest::class.java)
            val user = Mockito.mock(UserEntity::class.java)
            Mockito.`when`(exchange.request).thenReturn(serverRequest)
            Mockito.`when`(serverRequest.remoteAddress).thenReturn(null)
            Mockito.`when`(user.email).thenReturn(request.email)
            Mockito.`when`(userService.createUser(request)).thenReturn(user)

            val response = controller(legalDocumentsEnabled = false).register(exchange, request)

            assertEquals(200, response.statusCode.value())
            assertEquals(request.email, response.body!!.email)
            Mockito.verify(userService).createUser(request)
        }

    @Test
    fun `keeps login available while registration is disabled`() =
        runTest {
            val exchange = Mockito.mock(ServerWebExchange::class.java)
            val request = Mockito.mock(ServerHttpRequest::class.java)
            Mockito.`when`(exchange.request).thenReturn(request)
            Mockito.`when`(request.headers).thenReturn(HttpHeaders())
            Mockito
                .`when`(authService.login("user@example.com", "password", null, null))
                .thenReturn(LoginResult("access", "refresh", 3600))

            val response = controller(registrationEnabled = false).login(exchange, LoginDto("user@example.com", "password"))

            assertEquals(200, response.statusCode.value())
            Mockito.verify(authService).login("user@example.com", "password", null, null)
        }

    private fun controller(
        registrationEnabled: Boolean = true,
        legalDocumentsEnabled: Boolean = false,
        planLimitsEnabled: Boolean = false,
    ) = AuthController(
        userService = userService,
        authService = authService,
        authProperties = AuthProperties(features = AuthFeatureFlags(registrationEnabled = registrationEnabled)),
        legalDocumentProperties = LegalDocumentProperties(enabled = legalDocumentsEnabled),
        planProperties = PlanProperties(enabled = planLimitsEnabled),
        captchaService = captchaService,
        openAuthEmailWorkflowService = emailWorkflowService,
        secureCookie = false,
    )

    private fun validRegisterRequest() =
        RegisterDto(
            email = "new@example.com",
            password = "password",
            firstName = "New",
            lastName = "User",
            lang = "en-US",
            defaultCurrency = "USD",
            tmz = "UTC",
            acceptTerms = true,
            acceptPrivacy = true,
        )
}
