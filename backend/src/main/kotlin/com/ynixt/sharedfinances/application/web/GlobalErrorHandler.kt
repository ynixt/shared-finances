package com.ynixt.sharedfinances.application.web

import com.ynixt.sharedfinances.application.web.dto.AppResponseErrorDto
import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.hibernate.validator.internal.engine.ConstraintViolationImpl
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
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

    @ExceptionHandler(BadCredentialsException::class)
    fun onBadCredentialsException(
        ex: BadCredentialsException,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<AppResponseErrorDto>> {
        logger.error(ex.message, ex)

        val body =
            AppResponseErrorDto(
                messageI18n = "apiErrors.login.invalidCredentials",
                alternativeMessage = ex.message,
                argsI18n = null,
            )

        return Mono.just(
            ResponseEntity
                .status(401)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body),
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun onWebExchangeBindException(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<List<AppResponseErrorDto>>> {
        val body =
            ex.bindingResult.fieldErrors.map { fe ->
                val argsI18nMap = buildArgsMap(fe)

                AppResponseErrorDto(
                    messageI18n = fe.defaultMessage,
                    alternativeMessage = "${fe.field}: ${fe.code}",
                    argsI18n = argsI18nMap,
                )
            }

        return Mono.just(
            ResponseEntity
                .status(ex.statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body),
        )
    }

    private fun buildArgsMap(fe: FieldError): MutableMap<String, String> {
        val argsI18nMap =
            linkedMapOf(
                "field" to fe.field,
                "value" to (fe.rejectedValue?.toString() ?: "null"),
            )

        val attributes = extractConstraintAttributes(fe)

        attributes["min"]?.let { argsI18nMap["min"] = it.toString() }
        attributes["max"]?.let { argsI18nMap["max"] = it.toString() }
        attributes["regexp"]?.let { argsI18nMap["regexp"] = it.toString() }

        return argsI18nMap
    }

    private fun extractConstraintAttributes(fe: FieldError): Map<String, Any> {
        try {
            val violation = fe.unwrap(ConstraintViolationImpl::class.java)

            return violation.constraintDescriptor.attributes
        } catch (_: Exception) {
            return emptyMap()
        }
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
