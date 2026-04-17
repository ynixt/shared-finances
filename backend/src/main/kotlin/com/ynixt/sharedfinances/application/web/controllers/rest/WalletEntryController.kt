package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.CursorPageDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.DeleteScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EditScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EntrySummaryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ListEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.ScheduledExecutionManagerRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.SummaryEntryRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferQuoteDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferQuoteRequestDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferRateDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.TransferRateRequestDto
import com.ynixt.sharedfinances.application.web.mapper.WalletEntryDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEventDtoMapper
import com.ynixt.sharedfinances.domain.extensions.CursorPageExtensions.mapCursorPageToDto
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.walletentry.ScheduledExecutionManagerService
import com.ynixt.sharedfinances.domain.services.walletentry.TransferQuoteRequest
import com.ynixt.sharedfinances.domain.services.walletentry.TransferRateRequest
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryEditService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntrySummaryService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryTransferQuoteService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
@RequestMapping("/wallet-entries")
@Tag(
    name = "Wallet Entry",
    description = "Operations related to interact with wallet entry, without care about your specialization, that logged user has access",
)
class WalletEntryController(
    private val walletEventDtoMapper: WalletEventDtoMapper,
    private val walletEntryDtoMapper: WalletEntryDtoMapper,
    private val walletEntryCreateService: WalletEntryCreateService,
    private val walletEntryEditService: WalletEntryEditService,
    private val walletEntryRemovalService: WalletEntryRemovalService,
    private val walletEventListService: WalletEventListService,
    private val walletEntrySummaryService: WalletEntrySummaryService,
    private val scheduledExecutionManagerService: ScheduledExecutionManagerService,
    private val walletEntryTransferQuoteService: WalletEntryTransferQuoteService,
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

    @Operation(summary = "Resolve a suggested target amount for transfer")
    @PostMapping("/transfer-quote")
    suspend fun transferQuote(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: TransferQuoteRequestDto,
    ): TransferQuoteDto =
        walletEntryTransferQuoteService
            .quote(
                userId = principalToken.principal.id,
                request =
                    TransferQuoteRequest(
                        groupId = body.groupId,
                        originId = body.originId,
                        targetId = body.targetId,
                        date = body.date,
                        originValue = body.originValue,
                    ),
            ).let { result ->
                TransferQuoteDto(targetValue = result.targetValue)
            }

    @Operation(summary = "Resolve stored exchange rate for a transfer (nearest quote to the given date)")
    @PostMapping("/transfer-rate")
    suspend fun transferRate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: TransferRateRequestDto,
    ): TransferRateDto =
        walletEntryTransferQuoteService
            .transferRate(
                userId = principalToken.principal.id,
                request =
                    TransferRateRequest(
                        groupId = body.groupId,
                        originId = body.originId,
                        targetId = body.targetId,
                        date = body.date,
                    ),
            ).let { result ->
                TransferRateDto(
                    rate = result.rate,
                    quoteDate = result.quoteDate,
                    baseCurrency = result.baseCurrency,
                    quoteCurrency = result.quoteCurrency,
                )
            }

    @Operation(summary = "Edit one-off entry")
    @PutMapping("/{id}")
    suspend fun editOneOff(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody newEntryDto: NewEntryDto,
    ): ResponseEntity<Unit> =
        walletEntryEditService
            .editOneOff(
                userId = principalToken.principal.id,
                walletEventId = id,
                request = walletEntryDtoMapper.fromNewDtoToNewRequest(newEntryDto),
            ).let { ResponseEntity.noContent().build() }

    @Operation(summary = "Edit scheduled entry occurrence")
    @PutMapping("/scheduled/{recurrenceConfigId}")
    suspend fun editScheduled(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable recurrenceConfigId: UUID,
        @RequestBody body: EditScheduledEntryDto,
    ): ResponseEntity<Unit> =
        walletEntryEditService
            .editScheduled(
                userId = principalToken.principal.id,
                recurrenceConfigId = recurrenceConfigId,
                request = walletEntryDtoMapper.fromEditScheduledDtoToModel(body),
            ).let { ResponseEntity.noContent().build() }

    @Operation(summary = "Delete one-off entry")
    @DeleteMapping("/{id}")
    suspend fun deleteOneOff(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        walletEntryRemovalService
            .deleteOneOff(
                userId = principalToken.principal.id,
                walletEventId = id,
            ).let { ResponseEntity.noContent().build() }

    @Operation(summary = "Delete scheduled entry occurrence")
    @DeleteMapping("/scheduled/{recurrenceConfigId}")
    suspend fun deleteScheduled(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable recurrenceConfigId: UUID,
        @RequestBody body: DeleteScheduledEntryDto,
    ): ResponseEntity<Unit> =
        walletEntryRemovalService
            .deleteScheduled(
                userId = principalToken.principal.id,
                recurrenceConfigId = recurrenceConfigId,
                request = walletEntryDtoMapper.fromDeleteScheduledDtoToModel(body),
            ).let { ResponseEntity.noContent().build() }

    @Operation(summary = "Get one entry by id")
    @GetMapping("/{id}")
    suspend fun getById(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<EventForListDto> =
        walletEventListService
            .findById(
                userId = principalToken.principal.id,
                walletEventId = id,
            ).let { result ->
                ResponseEntity.ofNullable(result?.let(walletEventDtoMapper::fromListResponseToListDto))
            }

    @Operation(summary = "Get scheduled entry by recurrence config id")
    @GetMapping("/scheduled/{recurrenceConfigId}")
    suspend fun getScheduledByRecurrenceConfigId(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable recurrenceConfigId: UUID,
    ): ResponseEntity<EventForListDto> =
        walletEventListService
            .findScheduledByRecurrenceConfigId(
                userId = principalToken.principal.id,
                recurrenceConfigId = recurrenceConfigId,
            ).let { result ->
                ResponseEntity.ofNullable(result?.let(walletEventDtoMapper::fromListResponseToListDto))
            }

    @Operation(summary = "List entries")
    @PostMapping("/list")
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody listEntryRequest: ListEntryRequestDto,
    ): CursorPageDto<EventForListDto> =
        walletEventListService
            .list(
                userId = principalToken.principal.id,
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

    @Operation(summary = "List scheduled execution manager entries")
    @PostMapping("/scheduled-executions/list")
    suspend fun listScheduledExecutions(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody request: ScheduledExecutionManagerRequestDto,
    ): List<EventForListDto> =
        scheduledExecutionManagerService
            .list(
                userId = principalToken.principal.id,
                request = walletEntryDtoMapper.fromScheduledExecutionManagerRequestDtoToModel(request),
            ).map(walletEventDtoMapper::fromListResponseToListDto)
}
