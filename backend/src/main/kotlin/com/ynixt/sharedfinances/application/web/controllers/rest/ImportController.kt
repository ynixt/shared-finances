package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.imports.CreateImportDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportBatchDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportCategoryCatalogDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportDuplicateCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportGroupCategoryCatalogDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportHashCheckDto
import com.ynixt.sharedfinances.application.web.dto.imports.ImportPreferencesDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.application.web.validation.ImportLineLimitValidator
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportBatchSummary
import com.ynixt.sharedfinances.domain.models.imports.ImportDuplicateCheck
import com.ynixt.sharedfinances.domain.models.imports.ImportDuplicateLine
import com.ynixt.sharedfinances.domain.models.imports.ImportHashCheck
import com.ynixt.sharedfinances.domain.models.imports.ImportLine
import com.ynixt.sharedfinances.domain.models.imports.UndoImportResult
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletBeneficiaryLeg
import com.ynixt.sharedfinances.domain.services.imports.ImportCategoryCatalogService
import com.ynixt.sharedfinances.domain.services.imports.ImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
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
@RequestMapping("/imports")
@Tag(name = "Imports", description = "Transaction statement import and history operations")
class ImportController(
    private val importService: ImportService,
    private val importLineLimitValidator: ImportLineLimitValidator,
    private val importCategoryCatalogService: ImportCategoryCatalogService,
    private val categoryDtoMapper: CategoryDtoMapper,
) {
    @Operation(summary = "Get transaction import preferences")
    @GetMapping("/preferences")
    suspend fun preferences(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): ImportPreferencesDto = ImportPreferencesDto(maxLines = importLineLimitValidator.maximum(principalToken.principal.role))

    @Operation(summary = "Get the complete category catalog required by the import preview")
    @GetMapping("/category-catalog")
    suspend fun categoryCatalog(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): ImportCategoryCatalogDto {
        val categories = importCategoryCatalogService.findAll(principalToken.principal.id)
        val membersByGroup = importCategoryCatalogService.findAllMembers(principalToken.principal.id).groupBy { it.groupId }
        val categoriesByGroup = categories.filter { it.groupId != null }.groupBy { it.groupId!! }
        return ImportCategoryCatalogDto(
            personal = categories.filter { it.groupId == null }.map(categoryDtoMapper::toDto),
            groups =
                (categoriesByGroup.keys + membersByGroup.keys).map { groupId ->
                    ImportGroupCategoryCatalogDto(
                        groupId = groupId,
                        categories = categoriesByGroup[groupId].orEmpty().map(categoryDtoMapper::toDto),
                        members =
                            membersByGroup[groupId].orEmpty().map { groupUser ->
                                val user = requireNotNull(groupUser.user)
                                UserSimpleDto(user.id!!, user.firstName, user.lastName, user.email, user.photoUrl)
                            },
                    )
                },
        )
    }

    @Operation(summary = "Check whether a SHA-256 file hash was imported before")
    @GetMapping("/check-hash/{hash}")
    suspend fun checkHash(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable hash: String,
    ): ImportHashCheckDto =
        importService
            .checkHash(principalToken.principal.id, hash)
            .toDto()

    @Operation(summary = "Find exact duplicate transaction rows")
    @PostMapping("/check-duplicates")
    suspend fun checkDuplicates(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: ImportDuplicateCheckDto,
    ): List<Int> {
        importLineLimitValidator.validate(principalToken.principal.role, body.lines.size)
        return importService.checkDuplicates(
            userId = principalToken.principal.id,
            request =
                ImportDuplicateCheck(
                    lines =
                        body.lines.map { line ->
                            ImportDuplicateLine(
                                walletItemId = line.walletItemId,
                                name = line.name,
                                value = line.value,
                                date = line.date,
                                installment = line.installment,
                                externalTransactionId = line.externalTransactionId,
                            )
                        },
                ),
        )
    }

    @Operation(summary = "Accept an import batch for asynchronous processing")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: CreateImportDto,
    ): ImportBatchDto {
        importLineLimitValidator.validate(principalToken.principal.role, body.lines.size)
        return importService
            .create(
                userId = principalToken.principal.id,
                request =
                    CreateImport(
                        fileHash = body.fileHash,
                        fileName = body.fileName,
                        format = body.format,
                        lines =
                            body.lines.map { line ->
                                ImportLine(
                                    walletItemId = line.walletItemId,
                                    name = line.name,
                                    value = line.value,
                                    date = line.date,
                                    confirmed = line.confirmed,
                                    categoryId = line.categoryId,
                                    groupId = line.groupId,
                                    beneficiaries =
                                        line.beneficiaries?.map { beneficiary ->
                                            NewWalletBeneficiaryLeg(
                                                userId = beneficiary.userId,
                                                benefitPercent = beneficiary.benefitPercent,
                                            )
                                        },
                                    billDate = line.billDate,
                                    installment = line.installment,
                                    installmentTotal = line.installmentTotal,
                                    createPreviousInstallments = line.createPreviousInstallments,
                                    createFollowingInstallments = line.createFollowingInstallments,
                                    tags = line.tags,
                                    observations = line.observations,
                                    externalTransactionId = line.externalTransactionId,
                                    transferGroupId = line.transferGroupId,
                                    seriesGroupId = line.seriesGroupId,
                                )
                            },
                    ),
            ).toDto()
    }

    @Operation(summary = "Get an import batch")
    @GetMapping("/{id}")
    suspend fun get(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<ImportBatchDto> =
        importService.get(principalToken.principal.id, id)?.let { ResponseEntity.ok(it.toDto()) }
            ?: ResponseEntity.notFound().build()

    @Operation(summary = "List import history")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): List<ImportBatchDto> = importService.list(principalToken.principal.id).map { it.toDto() }

    @Operation(summary = "Undo an import batch")
    @DeleteMapping("/{id}")
    suspend fun undo(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<ImportBatchDto> =
        when (val result = importService.undo(principalToken.principal.id, id)) {
            is UndoImportResult.Accepted -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result.batch.toDto())
            UndoImportResult.NotFound -> ResponseEntity.notFound().build()
            UndoImportResult.InvalidStatus -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

    private fun ImportHashCheck.toDto() =
        ImportHashCheckDto(
            status = status,
            batchId = batchId,
            importedAt = importedAt,
            fileName = fileName,
        )

    private fun ImportBatchSummary.toDto() =
        ImportBatchDto(
            id = id,
            fileHash = fileHash,
            fileName = fileName,
            format = format,
            walletItemId = walletItemId,
            walletItemName = walletItemName,
            qty = qty,
            totalCredit = totalCredit,
            totalDebit = totalDebit,
            status = status,
            errorMessage = errorMessage,
            createdAt = createdAt,
            startedAt = startedAt,
            finishedAt = finishedAt,
            retries = retries,
        )
}
