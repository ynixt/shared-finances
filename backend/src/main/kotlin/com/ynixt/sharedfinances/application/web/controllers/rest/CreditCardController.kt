package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.EditCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.CreditCardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/credit-cards")
@Tag(
    name = "Credit Cards",
    description = "Operations related to all credit cards that logged user has access",
)
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val creditCardDtoMapper: CreditCardDtoMapper,
) {
    @Operation(summary = "Get all credit cards")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Page<CreditCardDto> =
        creditCardService
            .findAll(
                principalToken.principal.id,
                pageable,
            ).map(creditCardDtoMapper::toDto)

    @Operation(summary = "Get credit card by id")
    @GetMapping("/{id}")
    suspend fun findOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<CreditCardDto> =
        creditCardService
            .findOne(
                userId = principalToken.principal.id,
                id = id,
            ).let { creditCard ->
                ResponseEntity.ofNullable(creditCard?.let { creditCardDtoMapper.toDto(it) })
            }

    @Operation(summary = "Create a new credit card")
    @PostMapping
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewCreditCardDto,
    ): CreditCardDto =
        creditCardService
            .create(
                principalToken.principal.id,
                creditCardDtoMapper.fromNewDtoToNewRequest(body),
            ).let(creditCardDtoMapper::toDto)

    @Operation(summary = "Edit credit card by id")
    @PutMapping("/{id}")
    suspend fun edit(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditCreditCardDto,
    ): ResponseEntity<CreditCardDto> =
        creditCardService
            .edit(
                userId = principalToken.principal.id,
                id = id,
                creditCardDtoMapper.fromEditDtoToEditRequest(body),
            ).let { creditCard ->
                ResponseEntity.ofNullable(creditCard?.let { creditCardDtoMapper.toDto(it) })
            }

    @Operation(summary = "Delete credit card by id")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        creditCardService
            .delete(
                userId = principalToken.principal.id,
                id = id,
            ).let { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
