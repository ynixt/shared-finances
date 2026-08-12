package com.ynixt.sharedfinances.resources.services.exports

import com.ynixt.sharedfinances.domain.models.exports.TransactionExportCursor
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionCsvWriterTest {
    @Test
    fun `escapes delimiters quotes and line breaks`() =
        runTest {
            val writer = TransactionCsvWriter()
            val eventId = UUID.randomUUID()
            val row =
                TransactionExportRow(
                    origin = "wallet-id",
                    originName = "Checking",
                    date = LocalDate.of(2026, 8, 11),
                    description = "market; \"weekly\"",
                    value = BigDecimal("-42.30"),
                    currency = "BRL",
                    category = "category-id",
                    categoryName = "Food",
                    categoryConceptId = "food-concept-id",
                    group = null,
                    groupName = null,
                    installment = null,
                    beneficiaries = null,
                    bill = null,
                    tags = listOf("home", "food"),
                    observations = "first line\nsecond line",
                    confirmed = true,
                    transactionId = eventId.toString(),
                    transferId = null,
                    seriesId = null,
                    cursor = TransactionExportCursor(LocalDate.of(2026, 8, 11), eventId, UUID.randomUUID()),
                )

            val csv =
                writer
                    .write(flowOf(row))
                    .toList()
                    .fold(byteArrayOf()) { result, bytes -> result + bytes }
                    .decodeToString()

            assertTrue(csv.startsWith("\uFEFForigin;origin_name;date;description"))
            assertTrue(csv.contains("\"market; \"\"weekly\"\"\""))
            assertTrue(csv.contains("\"first line\nsecond line\""))
            assertTrue(csv.contains(";home,food;"))
        }

    @Test
    fun `matches the versioned fixture consumed by the real frontend importer`() =
        runTest {
            val bytes =
                TransactionCsvWriter()
                    .write(
                        flowOf(
                            *fixtureRows().toTypedArray(),
                        ),
                    ).toList()
                    .fold(byteArrayOf()) { all, chunk ->
                        all + chunk
                    }
            val fixture =
                Path.of(
                    "../frontend/src/app/pages/finances/transactions-page/" +
                        "import-transactions-page/fixtures/transaction-export-round-trip.csv",
                )

            val regenerationInstructions =
                "Set $REGENERATE_FIXTURE_ENV=true and rerun this test to regenerate the fixture intentionally."
            if (regenerateFixture()) {
                Files.createDirectories(fixture.parent)
                Files.write(fixture, bytes)
            }

            assertTrue(Files.exists(fixture), "Versioned CSV fixture is missing. $regenerationInstructions")
            assertContentEquals(
                Files.readAllBytes(fixture),
                bytes,
                "CSV writer output drifted. $regenerationInstructions",
            )
            assertEquals(6, fixtureRows().size)
            assertTrue(Files.readString(fixture).contains("home,food"))
        }

    private fun regenerateFixture(): Boolean =
        System.getenv(REGENERATE_FIXTURE_ENV).equals("true", ignoreCase = true) ||
            System.getProperty(REGENERATE_FIXTURE_PROPERTY).toBoolean()

    private fun fixtureRows(): List<TransactionExportRow> {
        val date = LocalDate.of(2026, 8, 11)

        fun row(
            entry: String,
            originName: String,
            value: String,
            transactionId: String,
            description: String,
            installment: String? = null,
            beneficiaries: String? = null,
            tags: List<String> = emptyList(),
            observations: String? = null,
            confirmed: Boolean = true,
            transferId: String? = null,
            seriesId: String? = null,
        ) = TransactionExportRow(
            origin = "source-${originName.lowercase()}",
            originName = originName,
            date = date,
            description = description,
            value = BigDecimal(value),
            currency = "BRL",
            category = "source-food",
            categoryName = "Food",
            categoryConceptId = "food-concept",
            group = "source-household",
            groupName = "Household",
            installment = installment,
            beneficiaries = beneficiaries,
            bill = null,
            tags = tags,
            observations = observations,
            confirmed = confirmed,
            transactionId = transactionId,
            transferId = transferId,
            seriesId = seriesId,
            cursor =
                TransactionExportCursor(
                    date,
                    UUID.nameUUIDFromBytes(transactionId.toByteArray()),
                    UUID.nameUUIDFromBytes(entry.toByteArray()),
                ),
        )

        return listOf(
            row(
                entry = "simple-entry",
                originName = "Checking",
                value = "-42.30",
                transactionId = "simple-transaction",
                description = "market; \"weekly\"\nrun",
                beneficiaries = "alice@example.com:60|bob@example.com:40",
                tags = listOf("home", "food"),
                observations = "first line\nsecond; \"quoted\" line",
            ),
            row(
                "transfer-debit",
                "Checking",
                "-100.00",
                "transfer-source",
                "Transfer out",
                confirmed = false,
                transferId = "transfer-1",
            ),
            row(
                "transfer-credit",
                "Savings",
                "100.00",
                "transfer-source",
                "Transfer in",
                confirmed = false,
                transferId = "transfer-1",
            ),
            row(
                "series-1",
                "Card",
                "-25.00",
                "installment-1",
                "Installment purchase",
                "1/12",
                seriesId = "series-1",
            ),
            row(
                "series-2",
                "Card",
                "-25.00",
                "installment-2",
                "Installment purchase",
                "2/12",
                seriesId = "series-1",
            ),
            row(
                "series-3",
                "Card",
                "-25.00",
                "installment-3",
                "Installment purchase",
                "3/12",
                seriesId = "series-1",
            ),
        )
    }

    private companion object {
        const val REGENERATE_FIXTURE_ENV = "REGENERATE_TRANSACTION_EXPORT_FIXTURE"
        const val REGENERATE_FIXTURE_PROPERTY = "sharedFinances.regenerateTransactionExportFixture"
    }
}
