package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.EditCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapPage
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
import reactor.core.publisher.Mono
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
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Mono<Page<CreditCardDto>> =
        creditCardService
            .findAll(
                principalToken.principal.id,
                pageable,
            ).mapPage(creditCardDtoMapper::toDto)

    @Operation(summary = "Get credit card by id")
    @GetMapping("/{id}")
    fun findOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<CreditCardDto>> =
        creditCardService
            .findOne(
                userId = principalToken.principal.id,
                id = id,
            ).map {
                ResponseEntity.ofNullable(creditCardDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Create a new credit card")
    @PostMapping
    fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewCreditCardDto,
    ): Mono<CreditCardDto> =
        creditCardService
            .create(
                principalToken.principal.id,
                creditCardDtoMapper.fromNewDtoToNewRequest(body),
            ).map(creditCardDtoMapper::toDto)

    @Operation(summary = "Edit credit card by id")
    @PutMapping("/{id}")
    fun edit(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditCreditCardDto,
    ): Mono<ResponseEntity<CreditCardDto>> =
        creditCardService
            .edit(
                userId = principalToken.principal.id,
                id = id,
                creditCardDtoMapper.fromEditDtoToEditRequest(body),
            ).map {
                ResponseEntity.ofNullable(creditCardDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Delete credit card by id")
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<Unit>> =
        creditCardService
            .delete(
                userId = principalToken.principal.id,
                id = id,
            ).map { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
