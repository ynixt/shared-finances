package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemForEntryListDto
import com.ynixt.sharedfinances.application.web.dto.wallet.WalletItemSearchResponseDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.application.web.mapper.WalletItemDtoMapper
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.TransferPurpose
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.models.WalletItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class WalletEntryDtoMapperImplTest {
    private val mapper = WalletEntryDtoMapperImpl(NoopWalletItemDtoMapper())

    @Test
    fun `transfer dto should default transfer purpose to general`() {
        val request =
            mapper.fromNewDtoToNewRequest(
                NewEntryDto(
                    type = WalletEntryType.TRANSFER,
                    groupId = UUID.randomUUID(),
                    originId = UUID.randomUUID(),
                    targetId = UUID.randomUUID(),
                    name = "Transfer",
                    categoryId = null,
                    date = LocalDate.of(2026, 4, 17),
                    value = null,
                    originValue = BigDecimal("100.00"),
                    targetValue = BigDecimal("100.00"),
                    confirmed = true,
                    observations = null,
                    paymentType = PaymentType.UNIQUE,
                    installments = null,
                    periodicity = null,
                    periodicityQtyLimit = null,
                    originBillDate = null,
                    targetBillDate = null,
                    tags = null,
                    transferPurpose = null,
                ),
            )

        assertThat(request.transferPurpose).isEqualTo(TransferPurpose.GENERAL)
    }

    @Test
    fun `non transfer dto should keep general transfer purpose`() {
        val request =
            mapper.fromNewDtoToNewRequest(
                NewEntryDto(
                    type = WalletEntryType.EXPENSE,
                    groupId = UUID.randomUUID(),
                    originId = null,
                    targetId = null,
                    sources =
                        listOf(
                            WalletSourceLegDto(
                                walletItemId = UUID.randomUUID(),
                                contributionPercent = BigDecimal("100.00"),
                                billDate = null,
                            ),
                        ),
                    name = "Expense",
                    categoryId = null,
                    date = LocalDate.of(2026, 4, 17),
                    value = BigDecimal("50.00"),
                    originValue = null,
                    targetValue = null,
                    confirmed = true,
                    observations = null,
                    paymentType = PaymentType.UNIQUE,
                    installments = null,
                    periodicity = null,
                    periodicityQtyLimit = null,
                    originBillDate = null,
                    targetBillDate = null,
                    tags = null,
                    transferPurpose = TransferPurpose.DEBT_SETTLEMENT,
                ),
            )

        assertThat(request.transferPurpose).isEqualTo(TransferPurpose.GENERAL)
    }

    private class NoopWalletItemDtoMapper : WalletItemDtoMapper {
        override fun searchResponseToDto(from: WalletItem): WalletItemSearchResponseDto = error("not used in this test")

        override fun entityToWalletItemForEntryListDto(from: WalletItem): WalletItemForEntryListDto = error("not used in this test")
    }
}
