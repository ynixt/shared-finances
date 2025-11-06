package com.ynixt.sharedfinances.application.web.controllers.rest.group.association

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapList
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/associations/creditCards")
@Tag(
    name = "Groups associations",
    description = "Operations related to credit card associations of groups that logged user has access",
)
class GroupCreditCardAssociationController(
    private val groupCreditCardAssociationService: GroupCreditCardAssociationService,
    private val creditCardDtoMapper: CreditCardDtoMapper,
) {
    @Operation(summary = "Get all credit cards that can be associate to this group")
    @GetMapping("/allowed")
    fun findAllAllowedCreditCardsToAssociate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
    ): Mono<List<CreditCardForGroupAssociateDto>> =
        groupCreditCardAssociationService
            .findAllAllowedCreditCardsToAssociate(
                userId = principalToken.principal.id,
                groupId = groupId,
            ).mapList(creditCardDtoMapper::toAssociateDto)

    @Operation(summary = "Get all associated credit cards of this group")
    @GetMapping
    fun findAllAssociatedCreditCards(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
    ): Mono<List<CreditCardForGroupAssociateDto>> =
        groupCreditCardAssociationService
            .findAllAssociatedCreditCards(
                userId = principalToken.principal.id,
                groupId = groupId,
            ).mapList(creditCardDtoMapper::toAssociateDto)
            .defaultIfEmpty(listOf())

    @Operation(summary = "Associate a new credit card to this group")
    @PutMapping("/{creditCardId}")
    fun associateCreditCard(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable creditCardId: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupCreditCardAssociationService
            .associateCreditCard(
                userId = principalToken.principal.id,
                groupId = groupId,
                creditCardId = creditCardId,
            ).map { ResponseEntity.noContent().build<Unit>() }
            .defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Unassociate a credit card at this group")
    @DeleteMapping("/{creditCardId}")
    fun unassociateCreditCard(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable creditCardId: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupCreditCardAssociationService
            .unassociateCreditCard(
                userId = principalToken.principal.id,
                groupId = groupId,
                creditCardId = creditCardId,
            ).map { ResponseEntity.noContent().build<Unit>() }
            .defaultIfEmpty(ResponseEntity.notFound().build())
}
