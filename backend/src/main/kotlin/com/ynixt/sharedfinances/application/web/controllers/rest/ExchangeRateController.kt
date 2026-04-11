package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteDto
import com.ynixt.sharedfinances.application.web.dto.exchangerate.ExchangeRateQuoteListRequestDto
import com.ynixt.sharedfinances.application.web.mapper.ExchangeRateQuoteDtoMapper
import com.ynixt.sharedfinances.domain.extensions.CursorPageExtensions.mapCursorPageToDto
import com.ynixt.sharedfinances.domain.models.CursorPageRequest
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
}
