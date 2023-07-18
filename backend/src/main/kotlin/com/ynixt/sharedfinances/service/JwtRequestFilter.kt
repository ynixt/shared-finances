package com.ynixt.sharedfinances.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.ynixt.sharedfinances.entity.User
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtRequestFilter(
    private val securityService: SecurityService,
    private val firebaseAuth: FirebaseAuth,
    private val userService: UserService
) :
    OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        authorize(request)
        filterChain.doFilter(request, response)
    }

    private fun authorize(request: HttpServletRequest) {
        var sessionCookieValue: String? = null
        var decodedToken: FirebaseToken? = null
        // Token verification

        val token = securityService.getBearerToken(request)
        try {
            if (token != null && token != "null"
                && !token.equals("undefined", ignoreCase = true)
            ) {
                decodedToken = firebaseAuth.verifyIdToken(token)
            }
        } catch (e: FirebaseAuthException) {
            logger.error("Firebase Exception:: " + e.localizedMessage, e)
        }

        val user = firebaseTokenToUser(decodedToken)

        if (user != null) {
            val context: SecurityContext = SecurityContextHolder.createEmptyContext()
            val authToken = UsernamePasswordAuthenticationToken(
                user, null, user.authorities
            )
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            context.authentication = authToken
            SecurityContextHolder.setContext(context)
        }
    }

    private fun firebaseTokenToUser(decodedToken: FirebaseToken?): User? {
        var user: User? = null

        if (decodedToken != null) {
            user = userService.createUserIfNotExists(decodedToken)
        }

        return user
    }
}