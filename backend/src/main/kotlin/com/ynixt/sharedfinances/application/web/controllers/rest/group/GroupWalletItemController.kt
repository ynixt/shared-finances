package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupWalletItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/wallet-items")
@Tag(
    name = "Wallet Item",
    description = "Operations related to interact with wallet item, without care about your implementation, that belongs to a group",
)
class GroupWalletItemController(
    private val groupWalletItemService: GroupWalletItemService,
    private val walletItemDtoMapper: WalletItemDtoMapper,
) {
    @Operation(summary = "Get all wallet items")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        pageable: Pageable,
    ): Page<WalletItemSearchResponseDto> =
        groupWalletItemService
            .findAllItems(
                userId = principalToken.principal.id,
                groupId = groupId,
                pageable,
            ).map(walletItemDtoMapper::searchResponseToDto)
}
