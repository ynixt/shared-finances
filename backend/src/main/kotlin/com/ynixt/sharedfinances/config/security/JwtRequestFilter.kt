package com.ynixt.sharedfinances.config.security

import com.ynixt.sharedfinances.service.SecurityService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class JwtRequestFilter(
    private val securityService: SecurityService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain
    ) {
        var authorizationSuccess = false

        try {
            authorize(request)
            authorizationSuccess = true
        } catch (ex: Exception) {
            logger.error("Exception during authorization: " + ex.localizedMessage, ex)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The token is not valid.")
        }

        if (authorizationSuccess) {
            filterChain.doFilter(request, response)
        }
    }

    private fun authorize(request: HttpServletRequest) {
        val token = securityService.getBearerToken(request)

        if (token != null && token != "null" && !token.equals("undefined", ignoreCase = true)) {
            val user = securityService.getUserFromBearerToken(token)

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
    }
}