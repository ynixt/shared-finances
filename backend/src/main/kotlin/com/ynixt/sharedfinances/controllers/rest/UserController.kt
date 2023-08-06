package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.model.dto.user.UserSettingsDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserController(
    private val securityService: SecurityService, private val userService: UserService
) {
    @PostMapping("settings")
    fun updateSettings(authentication: Authentication, @RequestBody newSettingsDto: UserSettingsDto) {
        val user = securityService.authenticationToUser(authentication)!!
        userService.updateSettings(user, newSettingsDto)
    }

    @DeleteMapping
    fun deleteAccount(authentication: Authentication) {
        val user = securityService.authenticationToUser(authentication)!!
        userService.deleteAccount(user)
    }
}
