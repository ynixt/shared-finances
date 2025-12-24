package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.auth.LoginDto
import com.ynixt.sharedfinances.application.web.dto.auth.LoginResultDto
import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.AuthService
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import com.ynixt.sharedfinances.domain.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.mono
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
import reactor.core.publisher.Mono
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
    fun register(
        @Valid @RequestBody payload: RegisterDto,
    ): Mono<ResponseEntity<Void>> =
        mono {
            userService.createUser(payload)
        }.thenReturn(ResponseEntity.ok().build())

    @Operation(
        summary = "Login using email/password",
    )
    @PostMapping("/open/auth/login")
    fun login(
        exchange: ServerWebExchange,
        @Valid @RequestBody payload: LoginDto,
    ): Mono<ResponseEntity<LoginResultDto>> {
        val request = exchange.request

        return authService
            .login(
                email = payload.email,
                passwordHash = payload.passwordHash,
                ip = request.remoteAddress?.address,
                userAgent = request.headers.getFirst("User-Agent"),
            ).map { loginResult ->
                val refreshCookie = setRefreshCookie(loginResult.refreshToken, Duration.ofSeconds(loginResult.refreshExpiresInSeconds))

                val responseBuilder =
                    ResponseEntity
                        .noContent()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResult.accessToken}")

                responseBuilder.build()
            }
    }

    @Operation(
        summary = "Logout user",
    )
    @PostMapping("/auth/logout")
    fun logout(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): Mono<ResponseEntity<Unit>> {
        val session = principalToken.getCredentials().claims[SESSION_CLAIM_NAME] as String

        println(refreshToken)

        return authService
            .logout(UUID.fromString(session))
            .thenReturn(
                ResponseEntity
                    .noContent()
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .build(),
            )
    }

    @Operation(
        summary = "Refresh user token",
    )
    @PostMapping("/open/auth/refresh")
    fun refreshToken(
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = true) refreshToken: String,
    ): Mono<ResponseEntity<Unit>> =
        authService
            .refreshToken(refreshToken)
            .map { refreshTokenResult ->
                ResponseEntity
                    .noContent()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $refreshTokenResult")
                    .build<Unit>()
            }.onErrorResume { error ->
                if (error is BadCredentialsException) {
                    Mono.just(
                        ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                            .build(),
                    )
                } else {
                    Mono.error(error)
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
}
