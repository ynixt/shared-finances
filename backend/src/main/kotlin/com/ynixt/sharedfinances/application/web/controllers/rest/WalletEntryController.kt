package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/wallet-entries")
@Tag(
    name = "Wallet Entry",
    description = "Operations related to interact with wallet entry, without care about your specialization, that logged user has access",
)
class WalletEntryController(
    private val walletEntryDtoMapper: WalletEntryDtoMapper,
    private val walletEntryCreateService: WalletEntryCreateService,
) {
    @Operation(summary = "Create a new entry")
    @PostMapping
    fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody newEntryDto: NewEntryDto,
    ): Mono<*> =
        walletEntryCreateService.create(
            principalToken.principal.id,
            walletEntryDtoMapper.fromNewDtoToNewRequest(newEntryDto),
        )
}
