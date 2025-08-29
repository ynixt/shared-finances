package com.ynixt.sharedfinances.application.web.controllers

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.EditBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapPage
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.BankAccountService
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
@RequestMapping("/bank-accounts")
class BankAccountController(
    private val bankAccountService: BankAccountService,
    private val bankAccountDtoMapper: BankAccountDtoMapper,
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Mono<Page<BankAccountDto>> =
        bankAccountService
            .findAllBanks(
                principalToken.principal.id,
                pageable,
            ).mapPage(bankAccountDtoMapper::toDto)

    @PostMapping
    fun newBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewBankAccountDto,
    ): Mono<BankAccountDto> =
        bankAccountService
            .newBankAccount(
                principalToken.principal.id,
                bankAccountDtoMapper.fromNewDtoToNewRequest(body),
            ).map(bankAccountDtoMapper::toDto)

    @PutMapping("/{id}")
    fun editBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditBankAccountDto,
    ): Mono<ResponseEntity<BankAccountDto>> =
        bankAccountService
            .editBankAccount(
                userId = principalToken.principal.id,
                id = id,
                bankAccountDtoMapper.fromEditDtoToEditRequest(body),
            ).map {
                ResponseEntity.ofNullable(
                    if (it == null) null else bankAccountDtoMapper.toDto(it),
                )
            }

    @DeleteMapping("/{id}")
    fun deleteBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<Unit>> =
        bankAccountService
            .deleteBankAccount(
                userId = principalToken.principal.id,
                id = id,
            ).map { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
