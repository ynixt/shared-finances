package com.ynixt.sharedfinances.application.web.controllers.rest

import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.year
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardBillDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/credit-card-bills")
@Tag(
    name = "Credit Card Bills",
    description = "Operations related to all credit card bills that logged user has access",
)
class CreditCardBillController(
    private val creditCardBillService: CreditCardBillService,
    private val creditCardBillDtoMapper: CreditCardBillDtoMapper,
) {
    @GetMapping("/{id}/of/{year}/{month}")
    fun getBillForMonth(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable month: Int,
        @PathVariable year: Int,
    ): Mono<CreditCardBillDto> =
        creditCardBillService
            .getBillForMonth(
                userId = principalToken.principal.id,
                creditCardId = id,
                month = month,
                year = year,
            ).map(creditCardBillDtoMapper::toDto)

    @PutMapping("/{id}/closingDate/{closingDate}")
    fun changeClosingDate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable closingDate: LocalDate,
    ): Mono<ResponseEntity<Unit>> =
        creditCardBillService
            .changeClosingDate(
                userId = principalToken.principal.id,
                creditCardId = id,
                closingDate = closingDate,
            ).map { ResponseEntity.noContent().build() }

    @PutMapping("/{id}/dueDate/{dueDate}")
    fun changeDueDate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable dueDate: LocalDate,
    ): Mono<ResponseEntity<Unit>> =
        creditCardBillService
            .changeDueDate(
                userId = principalToken.principal.id,
                creditCardId = id,
                dueDate = dueDate,
            ).map { ResponseEntity.noContent().build() }
}
