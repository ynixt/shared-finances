package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.UserMapper
import com.ynixt.sharedfinances.model.dto.user.CurrentUserDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val userMapper: UserMapper,
    private val securityService: SecurityService
) {
    @GetMapping("current-user")
    fun login(authentication: Authentication): CurrentUserDto {
        val user = securityService.authenticationToUser(authentication)!!
        return userMapper.toCurrentUserDto(userService.getForCurrentUser(user.id!!)!!)
    }
}
