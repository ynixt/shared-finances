package com.ynixt.sharedfinances.application.web

import com.ynixt.sharedfinances.application.web.dto.AppResponseErrorDto
import com.ynixt.sharedfinances.domain.exceptions.AppResponseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestControllerAdvice
class GlobalErrorHandler {
    val logger = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    @ExceptionHandler(AppResponseException::class)
    fun onAppResponseException(
        ex: AppResponseException,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<AppResponseErrorDto>> {
        val body =
            AppResponseErrorDto(
                messageI18n = ex.messageI18n,
                alternativeMessage = ex.alternativeMessage,
                argsI18n = ex.argsI18n,
            )
        return Mono.just(
            ResponseEntity
                .status(ex.statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body),
        )
    }

    @ExceptionHandler(ErrorResponseException::class)
    fun onResponseException(
        ex: ErrorResponseException,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<AppResponseErrorDto>> {
        logger.error(ex.message, ex)

        val body =
            AppResponseErrorDto(
                messageI18n = null,
                alternativeMessage = ex.message,
                argsI18n = null,
            )
        return Mono.just(
            ResponseEntity
                .status(ex.statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body),
        )
    }

    @ExceptionHandler(Throwable::class)
    fun onGenericError(
        ex: Throwable,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<AppResponseErrorDto>> {
        logger.error(ex.message, ex)

        val body =
            AppResponseErrorDto(
                messageI18n = null,
                alternativeMessage = ex.message ?: "Internal server error",
                argsI18n = null,
            )
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body),
        )
    }
}
