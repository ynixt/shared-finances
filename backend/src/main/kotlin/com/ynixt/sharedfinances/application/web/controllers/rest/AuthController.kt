package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.auth.LoginDto
import com.ynixt.sharedfinances.application.web.dto.auth.LoginMfaDto
import com.ynixt.sharedfinances.application.web.dto.auth.LoginResultDto
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.exceptions.MfaIsNeededException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.AuthService
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import com.ynixt.sharedfinances.domain.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.time.Duration
import java.util.UUID

private const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

@RestController
@Tag(
    name = "Auth",
    description = "Operations related to authentication",
)
class AuthController(
    private val userService: UserService,
    private val authService: AuthService,
    @param:Value("\${app.security.secureCookie}") private val secureCookie: Boolean,
) {
    @Operation(
        summary = "Register a new user using email/password",
    )
    @PostMapping("/open/auth/register")
    suspend fun register(
        @Valid @RequestBody payload: RegisterDto,
    ): ResponseEntity<Unit> = userService.createUser(payload).let { ResponseEntity.noContent().build() }

    @Operation(
        summary = "Login using email/password",
    )
    @PostMapping("/open/auth/login")
    suspend fun login(
        exchange: ServerWebExchange,
        @Valid @RequestBody payload: LoginDto,
    ): ResponseEntity<LoginResultDto> {
        val request = exchange.request

        return try {
            authService
                .login(
                    email = payload.email,
                    rawPassword = payload.password,
                    ip = request.remoteAddress?.address,
                    userAgent = request.headers.getFirst("User-Agent"),
                ).let { loginResult ->
                    val refreshCookie = setRefreshCookie(loginResult.refreshToken, Duration.ofSeconds(loginResult.refreshExpiresInSeconds))

                    ResponseEntity
                        .ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.accessToken}")
                        .body(LoginResultDto())
                }
        } catch (ex: MfaIsNeededException) {
            ResponseEntity.ok(LoginResultDto(mfaChallengeId = ex.challengeId))
        }
    }

    @Operation(
        summary = "Login using MFA",
    )
    @PostMapping("/open/auth/mfa")
    suspend fun mfa(
        exchange: ServerWebExchange,
        @Valid @RequestBody payload: LoginMfaDto,
    ): ResponseEntity<Unit> {
        val request = exchange.request

        return authService
            .mfa(
                challengeId = payload.challengeId,
                code = payload.code,
                ip = request.remoteAddress?.address,
                userAgent = request.headers.getFirst("User-Agent"),
            ).let { loginResult ->
                val refreshCookie = setRefreshCookie(loginResult.refreshToken, Duration.ofSeconds(loginResult.refreshExpiresInSeconds))

                ResponseEntity
                    .noContent()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.accessToken}")
                    .build()
            }
    }

    @Operation(
        summary = "Logout user",
    )
    @PostMapping("/auth/logout")
    suspend fun logout(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
//        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): ResponseEntity<Unit> {
        val session = principalToken.getCredentials().claims[SESSION_CLAIM_NAME] as String

        authService.logout(UUID.fromString(session))

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
            .build()
    }

    @Operation(
        summary = "Refresh user token",
    )
    @PostMapping("/open/auth/refresh")
    suspend fun refreshToken(
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): ResponseEntity<Unit> =
        if (refreshToken == null) {
            unauthorized()
        } else {
            return try {
                authService
                    .refreshToken(refreshToken)
                    .let { refreshTokenResult ->
                        ResponseEntity
                            .noContent()
                            .header(HttpHeaders.AUTHORIZATION, "Bearer $refreshTokenResult")
                            .build<Unit>()
                    }
            } catch (_: BadCredentialsException) {
                unauthorized()
            }
        }

    private fun setRefreshCookie(
        value: String,
        duration: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(REFRESH_TOKEN_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")
            .path("/api/open/auth")
            .maxAge(duration)
            .build()

    private fun clearRefreshCookie() = setRefreshCookie("", Duration.ZERO)

    private fun unauthorized(): ResponseEntity<Unit> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
            .build()
}
