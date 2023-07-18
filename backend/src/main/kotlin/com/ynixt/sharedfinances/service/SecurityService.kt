package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.User
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
class SecurityService {
    fun getUser(): User? {
        var userPrincipal: User? = null
        val securityContext = SecurityContextHolder.getContext()
        val principal = securityContext.authentication.principal
        if (principal is User) {
            userPrincipal = principal as User
        }
        return userPrincipal
    }

    fun getBearerToken(request: HttpServletRequest): String? {
        var bearerToken: String? = null
        val authorization = request.getHeader("Authorization")
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            bearerToken = authorization.substring(7, authorization.length)
        }
        return bearerToken
    }
}