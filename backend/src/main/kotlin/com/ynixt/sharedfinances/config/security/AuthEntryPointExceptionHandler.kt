//package com.ynixt.sharedfinances.config.security
//
//import jakarta.servlet.http.HttpServletRequest
//import jakarta.servlet.http.HttpServletResponse
//import org.springframework.security.core.AuthenticationException
//import org.springframework.security.web.AuthenticationEntryPoint
//import org.springframework.web.bind.annotation.ControllerAdvice
//import org.springframework.web.bind.annotation.ExceptionHandler
//
//
//@ControllerAdvice
//class AuthEntryPointExceptionHandler : AuthenticationEntryPoint {
//
//    override fun commence(
//        request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException
//    ) {
//        // 401 HTTP Response
//        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You need to be logged in to do that.")
//    }
//
//    @ExceptionHandler(value = [AccessDeniedException::class])
//    fun commence(request: HttpServletRequest?, response: HttpServletResponse, exception: AccessDeniedException) {
//        // 403 HTTP Response
//        response.sendError(
//            HttpServletResponse.SC_FORBIDDEN, "You don't have permission to do that: " + exception.message
//        )
//    }
//
//    @ExceptionHandler(value = [Exception::class])
//    fun commence(request: HttpServletRequest?, response: HttpServletResponse, exception: Exception) {
//        // 500 HTTP Response
//        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + exception.message)
//    }
//}
