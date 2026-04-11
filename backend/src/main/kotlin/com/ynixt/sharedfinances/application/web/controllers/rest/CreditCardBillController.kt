package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardBillDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.PayCreditCardBillDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardBillDtoMapper
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.CreditCardBillPaymentService
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardBillSummaryService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
    private val creditCardBillPaymentService: CreditCardBillPaymentService,
    private val creditCardBillSummaryService: CreditCardBillSummaryService,
    private val creditCardBillDtoMapper: CreditCardBillDtoMapper,
) {
    @GetMapping("/{id}/of/{year}/{month}")
    suspend fun getBillForMonth(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable month: Int,
        @PathVariable year: Int,
    ): ResponseEntity<CreditCardBillDto> =
        try {
            creditCardBillSummaryService
                .getBillForMonth(
                    userId = principalToken.principal.id,
                    creditCardId = id,
                    month = month,
                    year = year,
                ).let { bill ->
                    ResponseEntity.ofNullable(creditCardBillDtoMapper.toDto(bill))
                }
        } catch (_: UnauthorizedException) {
            ResponseEntity.notFound().build()
        }

    @PutMapping("/{id}/closingDate/{closingDate}")
    suspend fun changeClosingDate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable closingDate: LocalDate,
    ): ResponseEntity<Unit> =
        creditCardBillService
            .changeClosingDate(
                userId = principalToken.principal.id,
                creditCardId = id,
                closingDate = closingDate,
            ).let { ResponseEntity.noContent().build() }

    @PutMapping("/{id}/dueDate/{dueDate}")
    suspend fun changeDueDate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @PathVariable dueDate: LocalDate,
    ): ResponseEntity<Unit> =
        creditCardBillService
            .changeDueDate(
                userId = principalToken.principal.id,
                creditCardId = id,
                dueDate = dueDate,
            ).let { ResponseEntity.noContent().build() }

    @PostMapping("/{billId}/payments")
    suspend fun payBill(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable billId: UUID,
        @RequestBody request: PayCreditCardBillDto,
    ): ResponseEntity<Unit> =
        creditCardBillPaymentService
            .payBill(
                userId = principalToken.principal.id,
                billId = billId,
                bankAccountId = request.bankAccountId,
                date = request.date,
                amount = request.amount,
                observations = request.observations,
            ).let { ResponseEntity.noContent().build() }
}
