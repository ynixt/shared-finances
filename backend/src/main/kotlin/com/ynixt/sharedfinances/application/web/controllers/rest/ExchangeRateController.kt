package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteListRequestDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolutionDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateResolveBatchRequestDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.extensions.CursorPageExtensions.mapCursorPageToDto
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/exchange-rates")
@Tag(
    name = "Exchange Rates",
    description = "Operations related to stored currency exchange rates",
)
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService,
    private val exchangeRateQuoteDtoMapper: ExchangeRateQuoteDtoMapper,
) {
    @Operation(summary = "List exchange rates (cursor pagination)")
    @PostMapping("/list")
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: ExchangeRateQuoteListRequestDto,
    ): CursorPageDto<ExchangeRateQuoteDto> {
        val pageRequest = body.pageRequest ?: CursorPageRequest()
        val listRequest =
            ExchangeRateQuoteListRequest(
                pageRequest = pageRequest,
                baseCurrency =
                    body.baseCurrency
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.uppercase(),
                quoteCurrency =
                    body.quoteCurrency
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.uppercase(),
                quoteDateFrom = body.quoteDateFrom,
                quoteDateTo = body.quoteDateTo,
            )
        return exchangeRateService
            .listQuotes(listRequest)
            .mapCursorPageToDto(exchangeRateQuoteDtoMapper::toDto)
    }

    @Operation(summary = "Resolve the closest stored exchange rate for multiple currency/date combinations")
    @PostMapping("/resolve")
    suspend fun resolve(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: ExchangeRateResolveBatchRequestDto,
    ): List<ExchangeRateResolutionDto> {
        val normalizedRequests =
            body.requests
                .take(500)
                .map { request ->
                    val fromCurrency = request.fromCurrency.trim().uppercase()
                    val toCurrency = request.toCurrency.trim().uppercase()
                    request to
                        ConversionRequest(
                            value = BigDecimal.ONE,
                            fromCurrency = fromCurrency,
                            toCurrency = toCurrency,
                            referenceDate = request.referenceDate,
                        )
                }
        val resolvedByRequest = exchangeRateService.resolveRateBatch(normalizedRequests.map { it.second })

        return normalizedRequests.map { (request, conversionRequest) ->
            val resolved = resolvedByRequest[conversionRequest]
            ExchangeRateResolutionDto(
                fromCurrency = conversionRequest.fromCurrency,
                toCurrency = conversionRequest.toCurrency,
                referenceDate = request.referenceDate,
                rate = resolved?.rate,
                quoteDate = resolved?.quoteDate,
            )
        }
    }
}
