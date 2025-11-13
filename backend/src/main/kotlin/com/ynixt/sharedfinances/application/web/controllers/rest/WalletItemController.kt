package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapPage
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.WalletItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/wallet-items")
@Tag(
    name = "Wallet Item",
    description = "Operations related to interact with wallet item, without care about your implementation, that logged user has access",
)
class WalletItemController(
    private val walletItemService: WalletItemService,
    private val walletItemDtoMapper: WalletItemDtoMapper,
) {
    @Operation(summary = "Get all wallet items")
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Mono<Page<WalletItemSearchResponseDto>> =
        walletItemService
            .findAllItems(
                principalToken.principal.id,
                pageable,
            ).mapPage(walletItemDtoMapper::searchResponseToDto)
}
