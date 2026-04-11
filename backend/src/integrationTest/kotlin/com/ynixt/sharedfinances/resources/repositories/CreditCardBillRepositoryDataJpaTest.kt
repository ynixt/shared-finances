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
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DataR2dbcTest
@ActiveProfiles("test")
@Import(SimpleEntityUuidBeforeConvertCallback::class)
class CreditCardBillRepositoryDataJpaTest : RepositoryDataR2dbcTestSupport() {
    @Autowired
    private lateinit var dbClient: DatabaseClient

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

    @Test
    fun `should use open due-date index for dashboard projected bill lookup`() {
        runBlocking {
            val user = userRepository.save(newUser()).awaitSingle()
            val card = walletItemRepository.save(newCreditCard(requireNotNull(user.id))).awaitSingle()
            val creditCardId = requireNotNull(card.id)

            seedOpenBillExplainDataset(creditCardId)

            val plan =
                explainQuery(
                    sql =
                        """
                        SELECT bill.*
                        FROM credit_card_bill bill
                        JOIN wallet_item creditCard ON creditCard.id = bill.credit_card_id
                        WHERE
                            creditCard.user_id = :userId
                            AND creditCard.type = 'CREDIT_CARD'
                            AND creditCard.enabled = true
                            AND creditCard.show_on_dashboard = true
                            AND bill.value < 0
                            AND bill.due_date >= :minimumDueDate
                            AND bill.due_date <= :maximumDueDate
                        """.trimIndent(),
                    bindings =
                        mapOf(
                            "userId" to requireNotNull(user.id),
                            "minimumDueDate" to LocalDate.of(2026, 4, 1),
                            "maximumDueDate" to LocalDate.of(2026, 4, 30),
                        ),
                )

            assertThat(plan)
                .contains("idx_credit_card_bill_open_due_date")
                .doesNotContain("Seq Scan on credit_card_bill")
        }
    }

    private suspend fun reactor.core.publisher.Mono<Long>.awaitUpdatedCount(): Long = awaitSingleOrNull() ?: 0L

    private suspend fun seedOpenBillExplainDataset(creditCardId: UUID) {
        dbClient
            .sql("DELETE FROM credit_card_bill")
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql(
                """
                INSERT INTO credit_card_bill (
                    id,
                    credit_card_id,
                    bill_date,
                    due_date,
                    closing_date,
                    paid,
                    value
                )
                SELECT
                    (
                        substr(md5(i::text), 1, 8) || '-' ||
                        substr(md5(i::text), 9, 4) || '-' ||
                        substr(md5(i::text), 13, 4) || '-' ||
                        substr(md5(i::text), 17, 4) || '-' ||
                        substr(md5(i::text), 21, 12)
                    )::uuid,
                    :creditCardId,
                    DATE '2020-01-01' + i,
                    DATE '2020-01-11' + i,
                    DATE '2020-01-05' + i,
                    FALSE,
                    CASE WHEN i % 3 = 0 THEN -100.00 ELSE 50.00 END
                FROM generate_series(0, 3999) AS s(i)
                """.trimIndent(),
            ).bind("creditCardId", creditCardId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        dbClient
            .sql("ANALYZE credit_card_bill")
            .fetch()
            .rowsUpdated()
            .awaitSingle()
    }

    private suspend fun explainQuery(
        sql: String,
        bindings: Map<String, Any>,
    ): String {
        var spec = dbClient.sql("EXPLAIN (COSTS OFF) $sql")
        for ((name, value) in bindings) {
            spec = spec.bind(name, value)
        }
        return spec
            .map { row, _ -> row.get(0, String::class.java)!! }
            .all()
            .collectList()
            .awaitSingle()
            .joinToString("\n")
    }
}
