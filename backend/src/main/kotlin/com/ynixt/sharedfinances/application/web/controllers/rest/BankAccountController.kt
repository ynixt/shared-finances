package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.EditBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.BankAccountService
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
@RequestMapping("/bank-accounts")
@Tag(
    name = "Bank Accounts",
    description = "Operations related to all bank accounts that logged user has access",
)
class BankAccountController(
    private val bankAccountService: BankAccountService,
    private val bankAccountDtoMapper: BankAccountDtoMapper,
) {
    @Operation(summary = "Get all bank accounts")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Page<BankAccountDto> =
        bankAccountService
            .findAllBanks(
                principalToken.principal.id,
                pageable,
            ).map(bankAccountDtoMapper::toDto)

    @Operation(summary = "Get bank account by id")
    @GetMapping("/{id}")
    suspend fun findBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<BankAccountDto> =
        bankAccountService
            .findBankAccount(
                userId = principalToken.principal.id,
                id = id,
            ).let { bankAccount ->
                ResponseEntity.ofNullable(bankAccount?.let { bankAccountDtoMapper.toDto(it) })
            }

    @Operation(summary = "Create a new bank account")
    @PostMapping
    suspend fun newBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewBankAccountDto,
    ): BankAccountDto =
        bankAccountService
            .newBankAccount(
                principalToken.principal.id,
                bankAccountDtoMapper.fromNewDtoToNewRequest(body),
            ).let(bankAccountDtoMapper::toDto)

    @Operation(summary = "Edit bank account by id")
    @PutMapping("/{id}")
    suspend fun editBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditBankAccountDto,
    ): ResponseEntity<BankAccountDto> =
        bankAccountService
            .editBankAccount(
                userId = principalToken.principal.id,
                id = id,
                bankAccountDtoMapper.fromEditDtoToEditRequest(body),
            ).let { bankAccount ->
                ResponseEntity.ofNullable(bankAccount?.let { bankAccountDtoMapper.toDto(it) })
            }

    @Operation(summary = "Delete bank account by id")
    @DeleteMapping("/{id}")
    suspend fun deleteBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        bankAccountService
            .deleteBankAccount(
                userId = principalToken.principal.id,
                id = id,
            ).let { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }
}
