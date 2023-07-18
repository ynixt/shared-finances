package com.ynixt.sharedfinances.controllers

import com.ynixt.sharedfinances.dto.UserDto
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/v1/auth")
class AuthController(private val securityService: SecurityService) {
    @GetMapping("login")
    fun login(principal: Principal): UserDto {
        return securityService.getUser()!!.toUserDto()
    }
}