package com.ynixt.sharedfinances.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.entity.UserPrincipal
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.security.Principal

@Service
class SecurityService(private val firebaseAuth: FirebaseAuth, private val userService: UserService) {
    companion object {
        const val AuthorizationHeader = "Authorization"
    }

    fun principalToUser(principal: Principal?): User? {
        return if (principal == null) null else when (principal) {
            is UserPrincipal -> principal.user
            is UsernamePasswordAuthenticationToken -> principal.principal as User
            else -> TODO()
        }
    }

    fun authenticationToUser(authentication: Authentication?): User? {
        return (authentication?.principal) as User?
    }

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
        val authorization = request.getHeader(AuthorizationHeader)

        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            bearerToken = authorization.substring(7, authorization.length)
        }

        return bearerToken
    }

    fun getBearerToken(stompHeaderAccessor: StompHeaderAccessor): String? {
        val nativeHeaders = stompHeaderAccessor.getHeader("nativeHeaders") as Map<String, List<String>>
        var bearerToken: String? = null

        if (nativeHeaders.containsKey(AuthorizationHeader)) {
            val authorization = nativeHeaders[AuthorizationHeader]!![0]
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                bearerToken = authorization.substring(7, authorization.length)
            }
        }

        return bearerToken
    }

    fun getUser(stompHeaderAccessor: StompHeaderAccessor): User? {
        val token = getBearerToken(stompHeaderAccessor)
        return getUserFromBearerToken(token)
    }

    fun getUserFromBearerToken(bearerToken: String?): User? {
        if (bearerToken == null) return null

        val decodedToken = firebaseAuth.verifyIdToken(bearerToken)
        return firebaseTokenToUser(decodedToken)
    }

    private fun firebaseTokenToUser(decodedToken: FirebaseToken?): User? {
        var user: User? = null

        if (decodedToken != null) {
            user = userService.createUserIfNotExists(decodedToken)
        }

        return user
    }
}
