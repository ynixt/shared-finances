package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.imports.CreateImport
import com.ynixt.sharedfinances.domain.models.imports.ImportLine
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.models.walletentry.NewWalletSourceLeg
import com.ynixt.sharedfinances.domain.repositories.ImportBatchRepository
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.ImportBatchDispatchRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ImportBatchExecutionService(
    private val importBatchRepository: ImportBatchRepository,
    private val dispatchRepository: ImportBatchDispatchRepository,
    private val walletEntryCreateService: WalletEntryCreateService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    @Transactional
    suspend fun execute(
        batchId: UUID,
        workerId: String,
    ) {
        val batch = importBatchRepository.findById(batchId).awaitSingle()
        check(batch.status == ImportBatchStatus.RUNNING && batch.workerId == workerId) {
            "Import batch is no longer owned by this worker."
        }
        val payload = requireNotNull(batch.requestPayload) { "Import batch payload is unavailable." }
        val request = objectMapper.readValue<CreateImport>(payload)

        request.lines.forEach { line ->
            createLine(
                userId = batch.userId,
                walletItemId = line.walletItemId,
                batchId = batchId,
                line = line,
            )
        }

        check(
            dispatchRepository
                .markCompleted(
                    batchId = batchId,
                    workerId = workerId,
                    finishedAt = OffsetDateTime.now(clock),
                ).awaitSingle() > 0,
        ) { "Import batch lease was lost before completion." }
    }

    private suspend fun createLine(
        userId: UUID,
        walletItemId: UUID,
        batchId: UUID,
        line: ImportLine,
    ) {
        validateInstallment(line)
        val number = line.installment
        val total = line.installmentTotal
        val segmentLength =
            if (number != null && total != null && line.createFollowingInstallments) {
                total - number + 1
            } else {
                1
            }

        val current =
            walletEntryCreateService.create(
                userId = userId,
                newEntryRequest =
                    line.toEntryRequest(
                        walletItemId = walletItemId,
                        batchId = batchId,
                        date = line.date,
                        billDate = line.billDate,
                        confirmed = line.confirmed,
                        installments = if (number == null) null else segmentLength,
                        seriesOffset = if (number == null) 0 else number - 1,
                        seriesQtyTotal = total,
                        seriesId = null,
                    ),
            ) ?: throw UnauthorizedException()

        if (number == null || total == null || !line.createPreviousInstallments || number == 1) {
            return
        }

        val seriesId =
            when (current) {
                is RecurrenceEventEntity -> current.seriesId
                is WalletEventEntity -> requireNotNull(current.recurrenceEvent).seriesId
                else -> error("Installment creation did not return recurrence metadata.")
            }
        val monthsBack = (number - 1).toLong()
        walletEntryCreateService.create(
            userId = userId,
            newEntryRequest =
                line.toEntryRequest(
                    walletItemId = walletItemId,
                    batchId = batchId,
                    date = line.date.minusMonths(monthsBack),
                    billDate = line.billDate?.minusMonths(monthsBack),
                    confirmed = true,
                    installments = number - 1,
                    seriesOffset = 0,
                    seriesQtyTotal = total,
                    seriesId = seriesId,
                    externalTransactionId = null,
                ),
        ) ?: throw UnauthorizedException()
    }

    private fun ImportLine.toEntryRequest(
        walletItemId: UUID,
        batchId: UUID,
        date: LocalDate,
        billDate: LocalDate?,
        confirmed: Boolean,
        installments: Int?,
        seriesOffset: Int,
        seriesQtyTotal: Int?,
        seriesId: UUID?,
        externalTransactionId: String? = this.externalTransactionId,
    ): NewEntryRequest {
        val type = if (value.signum() < 0) WalletEntryType.EXPENSE else WalletEntryType.REVENUE
        return NewEntryRequest(
            type = type,
            groupId = groupId,
            originId = walletItemId,
            targetId = null,
            name = name?.trim()?.ifBlank { null },
            categoryId = categoryId,
            date = date,
            value = value.abs(),
            confirmed = confirmed,
            observations = observations,
            paymentType = if (installments == null) PaymentType.UNIQUE else PaymentType.INSTALLMENTS,
            installments = installments,
            periodicity = if (installments == null) null else RecurrenceType.MONTHLY,
            seriesOffset = seriesOffset,
            seriesQtyTotal = seriesQtyTotal,
            seriesId = seriesId,
            importBatchId = batchId,
            externalTransactionId = externalTransactionId,
            recurrenceConfirmedOverride = confirmed,
            originBillDate = billDate,
            tags = tags,
            sources =
                listOf(
                    NewWalletSourceLeg(
                        walletItemId = walletItemId,
                        contributionPercent = BigDecimal("100.00"),
                        billDate = billDate,
                    ),
                ),
            beneficiaries = beneficiaries,
        )
    }

    private fun validateInstallment(line: ImportLine) {
        val number = line.installment
        val total = line.installmentTotal
        require((number == null) == (total == null)) { "Installment number and total must be provided together." }
        if (number != null && total != null) {
            require(number in 1..total) { "Installment number must be between 1 and the installment total." }
        }
    }
}
