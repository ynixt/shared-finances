package com.ynixt.sharedfinances.application.web.controllers.rest.group.association

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapList
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
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
@RequestMapping("/groups/{groupId}/associations/banks")
@Tag(
    name = "Groups associations",
    description = "Operations related to bank account associations of groups that logged user has access",
)
class GroupBankAccountAssociationController(
    private val groupBankAssociationService: GroupBankAssociationService,
    private val bankAccountDtoMapper: BankAccountDtoMapper,
) {
    @Operation(summary = "Get all bank accounts that can be associate to this group")
    @GetMapping("/allowed")
    fun findAllAllowedBanksToAssociate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
    ): Mono<List<BankAccountForGroupAssociateDto>> =
        groupBankAssociationService
            .findAllAllowedBanksToAssociate(
                userId = principalToken.principal.id,
                groupId = groupId,
            ).mapList(bankAccountDtoMapper::toAssociateDto)

    @Operation(summary = "Get all associated bank accounts of this group")
    @GetMapping
    fun findAllAssociatedBanks(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
    ): Mono<List<BankAccountForGroupAssociateDto>> =
        groupBankAssociationService
            .findAllAssociatedBanks(
                userId = principalToken.principal.id,
                groupId = groupId,
            ).mapList(bankAccountDtoMapper::toAssociateDto)
            .defaultIfEmpty(listOf())

    @Operation(summary = "Associate a new bank account to this group")
    @PutMapping("/{bankAccountId}")
    fun associateBank(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable bankAccountId: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupBankAssociationService
            .associateBank(
                userId = principalToken.principal.id,
                groupId = groupId,
                bankAccountId = bankAccountId,
            ).map { ResponseEntity.noContent().build<Unit>() }
            .defaultIfEmpty(ResponseEntity.notFound().build())

    @Operation(summary = "Unassociate a bank account at this group")
    @DeleteMapping("/{bankAccountId}")
    fun unassociateBank(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable bankAccountId: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupBankAssociationService
            .unassociateBank(
                userId = principalToken.principal.id,
                groupId = groupId,
                bankAccountId = bankAccountId,
            ).map { ResponseEntity.noContent().build<Unit>() }
            .defaultIfEmpty(ResponseEntity.notFound().build())
}
