package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class RecurrenceEventDtoMapperImplTest {
    private val mapper = RecurrenceEventDtoMapperImpl()

    @Test
    fun `should expose global installment total as qtyLimit for installment recurrence`() {
        val recurrence =
            RecurrenceEventEntity(
                name = "Installment",
                categoryId = null,
                createdByUserId = UUID.randomUUID(),
                groupId = null,
                tags = null,
                observations = null,
                type = WalletEntryType.EXPENSE,
                periodicity = RecurrenceType.MONTHLY,
                paymentType = PaymentType.INSTALLMENTS,
                qtyExecuted = 1,
                qtyLimit = 2,
                lastExecution = LocalDate.of(2026, 1, 8),
                nextExecution = LocalDate.of(2026, 2, 8),
                endExecution = LocalDate.of(2026, 3, 8),
                seriesId = UUID.randomUUID(),
                seriesOffset = 1,
            ).also {
                it.id = UUID.randomUUID()
                it.seriesQtyTotal = 3
            }

        val dto = mapper.toDto(recurrence)

        assertThat(dto.qtyLimit).isEqualTo(3)
    }

    @Test
    fun `should expose global total as qtyLimit for limited recurring recurrence`() {
        val recurrence =
            RecurrenceEventEntity(
                name = "Recurring",
                categoryId = null,
                createdByUserId = UUID.randomUUID(),
                groupId = null,
                tags = null,
                observations = null,
                type = WalletEntryType.EXPENSE,
                periodicity = RecurrenceType.MONTHLY,
                paymentType = PaymentType.RECURRING,
                qtyExecuted = 1,
                qtyLimit = 5,
                lastExecution = LocalDate.of(2026, 1, 8),
                nextExecution = LocalDate.of(2026, 2, 8),
                endExecution = LocalDate.of(2026, 6, 8),
                seriesId = UUID.randomUUID(),
                seriesOffset = 0,
            ).also {
                it.id = UUID.randomUUID()
                it.seriesQtyTotal = 8
            }

        val dto = mapper.toDto(recurrence)

        assertThat(dto.qtyLimit).isEqualTo(8)
    }

    @Test
    fun `should keep qtyLimit null for unlimited recurring recurrence`() {
        val recurrence =
            RecurrenceEventEntity(
                name = "Recurring",
                categoryId = null,
                createdByUserId = UUID.randomUUID(),
                groupId = null,
                tags = null,
                observations = null,
                type = WalletEntryType.EXPENSE,
                periodicity = RecurrenceType.MONTHLY,
                paymentType = PaymentType.RECURRING,
                qtyExecuted = 4,
                qtyLimit = null,
                lastExecution = LocalDate.of(2026, 4, 8),
                nextExecution = LocalDate.of(2026, 5, 8),
                endExecution = null,
                seriesId = UUID.randomUUID(),
                seriesOffset = 0,
            ).also {
                it.id = UUID.randomUUID()
                it.seriesQtyTotal = 4
            }

        val dto = mapper.toDto(recurrence)

        assertThat(dto.qtyLimit).isNull()
    }
}
