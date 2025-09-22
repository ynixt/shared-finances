package com.ynixt.sharedfinances.application.web.controllers

import com.ynixt.sharedfinances.application.web.dto.UserResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.mono
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/users")
@Tag(
    name = "Users",
    description = "Operations related to the current user",
)
class UserController(
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper,
) {
    @GetMapping("/current")
    @Operation(summary = "Get info about the logged user")
    fun currentUser(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): Mono<UserResponseDto> = Mono.just(userDtoMapper.toResponseDtoFromPrincipal(principalToken.principal))

    @PutMapping("/current/changeLanguage/{newLang}")
    @Operation(summary = "Change language of logged user")
    fun changeLanguage(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable("newLang") newLang: String,
    ): Mono<ResponseEntity<Unit>> =
        mono {
            userService.changeLanguage(principalToken.principal.id, newLang)
        }.thenReturn(ResponseEntity.ok().build())

    @PutMapping("/current/changeDefaultCurrency/{newDefaultCurrency}")
    @Operation(summary = "Change default currency of logged user")
    fun changeDefaultCurrency(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable("newDefaultCurrency") newDefaultCurrency: String,
    ): Mono<ResponseEntity<Unit>> =
        mono {
            userService.changeDefaultCurrency(principalToken.principal.id, newDefaultCurrency)
        }.thenReturn(ResponseEntity.ok().build())
}
