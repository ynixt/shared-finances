package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEventDtoMapper
import com.ynixt.sharedfinances.domain.extensions.CursorPageExtensions.mapCursorPageToDto
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntrySummaryService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet-entries")
@Tag(
    name = "Wallet Entry",
    description = "Operations related to interact with wallet entry, without care about your specialization, that logged user has access",
)
class WalletEntryController(
    private val walletEventDtoMapper: WalletEventDtoMapper,
    private val walletEntryDtoMapper: WalletEntryDtoMapper,
    private val walletEntryCreateService: WalletEntryCreateService,
    private val walletEventListService: WalletEventListService,
    private val walletEntrySummaryService: WalletEntrySummaryService,
) {
    @Operation(summary = "Create a new entry")
    @PostMapping
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody newEntryDto: NewEntryDto,
    ): ResponseEntity<Unit> =
        walletEntryCreateService
            .create(
                principalToken.principal.id,
                walletEntryDtoMapper.fromNewDtoToNewRequest(newEntryDto),
            ).let { ResponseEntity.noContent().build() }

    @Operation(summary = "List entries")
    @PostMapping("/list")
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody listEntryRequest: ListEntryRequestDto,
    ): CursorPageDto<EventForListDto> =
        walletEventListService
            .list(
                userId = principalToken.principal.id,
                groupId = null,
                request = listEntryRequest.let { walletEntryDtoMapper.fromEntryListRequestDtoToModel(it) },
            ).mapCursorPageToDto(walletEventDtoMapper::fromListResponseToListDto)

    @Operation(summary = "Summary entries")
    @PostMapping("/summary")
    suspend fun summary(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody request: SummaryEntryRequestDto,
    ): ResponseEntity<EntrySummaryDto> =
        walletEntrySummaryService
            .summary(
                userId = principalToken.principal.id,
                groupId = null,
                request = request.let { walletEntryDtoMapper.fromEntrySummaryRequestDtoToModel(it) },
            ).let { summary ->
                ResponseEntity.ofNullable(summary?.let(walletEntryDtoMapper::fromSummaryModelToDto))
            }
}
