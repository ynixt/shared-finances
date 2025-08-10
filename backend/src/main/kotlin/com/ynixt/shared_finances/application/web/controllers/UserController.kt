package com.ynixt.shared_finances.application.web.controllers

import com.ynixt.shared_finances.domain.models.dto.UserResponseDto
import com.ynixt.shared_finances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.shared_finances.domain.services.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {
    @GetMapping("/current")
    fun currentUser(@AuthenticationPrincipal principalToken: UserJwtAuthenticationToken): Mono<UserResponseDto> =
        Mono.just(UserResponseDto.fromPrincipal(principalToken.principal))
}
