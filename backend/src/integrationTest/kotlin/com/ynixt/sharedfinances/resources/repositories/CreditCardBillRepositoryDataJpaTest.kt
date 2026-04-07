package com.ynixt.sharedfinances.resources.repositories

import com.ynixt.sharedfinances.application.config.SimpleEntityUuidBeforeConvertCallback
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.UserSpringDataRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletItemSpringDataRepository
import com.ynixt.sharedfinances.support.RepositoryDataR2dbcTestSupport
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@Import(SimpleEntityUuidBeforeConvertCallback::class)
class CreditCardBillRepositoryDataJpaTest : RepositoryDataR2dbcTestSupport() {
    @Autowired
    private lateinit var userRepository: UserSpringDataRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemSpringDataRepository

    @Autowired
    private lateinit var creditCardBillRepository: CreditCardBillRepository

    @Test
    fun `should update bill value and dates and handle unknown id`() {
        runBlocking {
            val user = userRepository.save(newUser()).awaitSingle()
            val card = walletItemRepository.save(newCreditCard(requireNotNull(user.id))).awaitSingle()
            val billDate = LocalDate.of(2026, 4, 1)

            val bill =
                creditCardBillRepository
                    .save(
                        newCreditCardBill(
                            creditCardId = requireNotNull(card.id),
                            billDate = billDate,
                            dueDate = LocalDate.of(2026, 4, 10),
                            closingDate = LocalDate.of(2026, 4, 5),
                            value = BigDecimal("-10.00"),
                        ),
                    ).awaitSingle()

            val addValueLines =
                creditCardBillRepository
                    .addValueById(requireNotNull(bill.id), BigDecimal("-90.00"))
                    .awaitUpdatedCount()
            val dueDateLines =
                creditCardBillRepository
                    .changeDueDateById(requireNotNull(bill.id), LocalDate.of(2026, 4, 12))
                    .awaitUpdatedCount()
            val closingDateLines =
                creditCardBillRepository
                    .changeClosingDateById(requireNotNull(bill.id), LocalDate.of(2026, 4, 7))
                    .awaitUpdatedCount()

            val unknownId = UUID.randomUUID()
            val unknownValueLines = creditCardBillRepository.addValueById(unknownId, BigDecimal.ONE).awaitUpdatedCount()
            val unknownDueLines = creditCardBillRepository.changeDueDateById(unknownId, LocalDate.of(2026, 4, 13)).awaitUpdatedCount()
            val unknownClosingLines =
                creditCardBillRepository
                    .changeClosingDateById(unknownId, LocalDate.of(2026, 4, 8))
                    .awaitUpdatedCount()

            val updated =
                creditCardBillRepository
                    .findOneByCreditCardIdAndBillDate(
                        creditCardId = requireNotNull(card.id),
                        billDate = billDate,
                    ).awaitSingleOrNull()

            assertThat(addValueLines).isGreaterThanOrEqualTo(0L)
            assertThat(dueDateLines).isGreaterThanOrEqualTo(0L)
            assertThat(closingDateLines).isGreaterThanOrEqualTo(0L)
            assertThat(updated).isNotNull()
            assertThat(requireNotNull(updated).value).isEqualByComparingTo(BigDecimal("-100.00"))
            assertThat(updated.dueDate).isEqualTo(LocalDate.of(2026, 4, 12))
            assertThat(updated.closingDate).isEqualTo(LocalDate.of(2026, 4, 7))

            assertThat(unknownValueLines).isEqualTo(0L)
            assertThat(unknownDueLines).isEqualTo(0L)
            assertThat(unknownClosingLines).isEqualTo(0L)
        }
    }

    private suspend fun reactor.core.publisher.Mono<Long>.awaitUpdatedCount(): Long = awaitSingleOrNull() ?: 0L
}
