package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.exports.CreateExportDto
import com.ynixt.sharedfinances.application.web.dto.exports.ExportBatchDto
import com.ynixt.sharedfinances.domain.models.exports.CreateExport
import com.ynixt.sharedfinances.domain.models.exports.ExportBatchSummary
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.exports.ExportService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/exports")
class ExportController(
    private val exportService: ExportService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun create(
        @AuthenticationPrincipal token: UserJwtAuthenticationToken,
        @RequestBody body: CreateExportDto,
    ): ExportBatchDto =
        exportService
            .create(
                token.principal.id,
                token.principal.role,
                CreateExport(
                    body.format,
                    TransactionExportFilter(
                        groupId = body.filter.groupId,
                        dateFrom = body.filter.dateFrom,
                        dateTo = body.filter.dateTo,
                        walletItemIds = body.filter.walletItemIds,
                        categoryIds = body.filter.categoryIds,
                        entryTypes = body.filter.entryTypes,
                        tags = body.filter.tags,
                        confirmed = body.filter.confirmed,
                        billDateMode = body.filter.billDateMode,
                    ),
                ),
            ).toDto()

    @GetMapping("/{id}")
    suspend fun get(
        @AuthenticationPrincipal token: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<ExportBatchDto> =
        exportService.get(token.principal.id, id)?.let { ResponseEntity.ok(it.toDto()) } ?: ResponseEntity.notFound().build()

    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal token: UserJwtAuthenticationToken,
    ): List<ExportBatchDto> = exportService.list(token.principal.id).map { it.toDto() }

    @GetMapping("/{id}/download")
    suspend fun download(
        @AuthenticationPrincipal token: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val download = exportService.download(token.principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(download.contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition
                    .attachment()
                    .filename(download.fileName)
                    .build()
                    .toString(),
            ).header(FIRST_DOWNLOADED_AT_HEADER, download.firstDownloadedAt.toString())
            .header(DOWNLOAD_EXPIRES_AT_HEADER, download.downloadExpiresAt.toString())
            .body(download.resource)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(
        @AuthenticationPrincipal token: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> =
        if (exportService.delete(token.principal.id, id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()

    private fun ExportBatchSummary.toDto() =
        ExportBatchDto(
            id,
            format,
            status,
            rowCount,
            errorMessage,
            createdAt,
            startedAt,
            finishedAt,
            firstDownloadedAt,
            downloadExpiresAt,
            fileDeletedAt,
            downloadAvailable,
        )

    private companion object {
        const val FIRST_DOWNLOADED_AT_HEADER = "X-Export-First-Downloaded-At"
        const val DOWNLOAD_EXPIRES_AT_HEADER = "X-Export-Download-Expires-At"
    }
}
