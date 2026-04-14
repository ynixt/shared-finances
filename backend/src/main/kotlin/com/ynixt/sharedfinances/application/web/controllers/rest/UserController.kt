package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.auth.ChangePasswordDto
import com.ynixt.sharedfinances.application.web.dto.user.UpdateUserDto
import com.ynixt.sharedfinances.application.web.dto.user.UserOnboardingDto
import com.ynixt.sharedfinances.application.web.dto.user.UserResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.OnboardingService
import com.ynixt.sharedfinances.domain.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
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
    private val onboardingService: OnboardingService,
) {
    @GetMapping("/current")
    @Operation(summary = "Get info about the logged user")
    fun currentUser(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): UserResponseDto = userDtoMapper.toResponseDtoFromPrincipal(principalToken.principal)

    @PutMapping("/current")
    @Operation(summary = "Change default currency of logged user")
    suspend fun updateCurrentUser(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @Valid @RequestPart("dto") dto: Mono<UpdateUserDto>,
        @RequestPart("avatar", required = false) avatar: Mono<FilePart>,
    ): UserResponseDto =
        userService
            .updateUser(
                userId = principalToken.principal.id,
                updateUserDto = dto.awaitSingle(),
                newAvatar = avatar.awaitSingleOrNull(),
            ).let(userDtoMapper::toDto)

    @PutMapping("/current/changeLanguage/{newLang}")
    @Operation(summary = "Change language of logged user")
    suspend fun changeLanguage(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable newLang: String,
    ): ResponseEntity<Unit> = userService.changeLanguage(principalToken.principal.id, newLang).let { ResponseEntity.noContent().build() }

    @PutMapping("/current/changePassword")
    @Operation(summary = "Change default currency of logged user")
    suspend fun changePassword(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @Valid @RequestBody changePasswordDto: ChangePasswordDto,
    ): ResponseEntity<Unit> =
        userService
            .changePassword(
                userId = principalToken.principal.id,
                currentPasswordHash = changePasswordDto.currentPassword,
                newPasswordHash = changePasswordDto.newPassword,
            ).let { ResponseEntity.noContent().build() }

    @PostMapping("/current/onboarding")
    suspend fun onboarding(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: UserOnboardingDto,
    ): ResponseEntity<Unit> =
        onboardingService
            .onboarding(
                userId = principalToken.principal.id,
                onboardingDto = body,
            ).let { ResponseEntity.noContent().build() }

    @DeleteMapping("/current")
    @Operation(summary = "Permanently delete the authenticated user account")
    suspend fun deleteCurrentAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): ResponseEntity<Unit> =
        userService
            .deleteCurrentAccount(principalToken.principal.id)
            .let { ResponseEntity.noContent().build() }
}
