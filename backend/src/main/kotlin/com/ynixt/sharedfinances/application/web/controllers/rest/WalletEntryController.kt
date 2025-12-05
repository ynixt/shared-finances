package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntryForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapCursorPageToDto
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryListService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntrySummaryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
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
    private val walletEntryListService: WalletEntryListService,
    private val walletEntrySummaryService: WalletEntrySummaryService,
) {
    @Operation(summary = "Create a new entry")
    @PostMapping
    fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody newEntryDto: NewEntryDto,
    ): Mono<ResponseEntity<Unit>> =
        walletEntryCreateService
            .create(
                principalToken.principal.id,
                walletEntryDtoMapper.fromNewDtoToNewRequest(newEntryDto),
            ).thenReturn(ResponseEntity.noContent().build())

    @Operation(summary = "List entries")
    @PostMapping("/list")
    fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody listEntryRequest: ListEntryRequestDto,
    ): Mono<CursorPageDto<EntryForListDto>> =
        walletEntryListService
            .list(
                userId = principalToken.principal.id,
                groupId = null,
                request = listEntryRequest.let { walletEntryDtoMapper.fromEntryListRequestDtoToModel(it) },
            ).mapCursorPageToDto(walletEntryDtoMapper::fromListResponseToListDto)

    @Operation(summary = "Summary entries")
    @PostMapping("/summary")
    fun summary(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody request: SummaryEntryRequestDto,
    ): Mono<EntrySummaryDto> =
        walletEntrySummaryService
            .summary(
                userId = principalToken.principal.id,
                groupId = null,
                request = request.let { walletEntryDtoMapper.fromEntrySummaryRequestDtoToModel(it) },
            ).map(walletEntryDtoMapper::fromSummaryModelToDto)
}
