package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.application.web.dto.dashboard.GroupOverviewDashboardDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.EditScheduledEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.NewEntryDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletBeneficiaryLegDto
import com.ynixt.sharedfinances.application.web.dto.walletentry.WalletSourceLegDto
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.ScheduledEditScope
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GroupDepartureIntegrationTest : IntegrationTestContainers() {
    @Autowired private lateinit var webClient: WebTestClient

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var passwordEncoder: PasswordEncoder

    @Autowired private lateinit var groupService: GroupService

    @Autowired private lateinit var walletEntryCreateService: WalletEntryCreateService

    private lateinit var ownerUtil: UserTestUtil
    private lateinit var departingUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        ownerUtil = UserTestUtil(userRepository, passwordEncoder, webClient)
        departingUtil = UserTestUtil(userRepository, passwordEncoder, webClient)
    }

    @Test
    fun `departure ends schedules by wallet item and preserves history debt and attribution`() =
        runBlocking {
            val fixture = seedFixture()

            assertThat(groupService.leaveGroup(fixture.departingUserId, fixture.groupId)).isTrue()

            openConnection().use { connection ->
                assertThat(connection.count("SELECT COUNT(*) FROM group_user WHERE group_id = '${fixture.groupId}'"))
                    .isEqualTo(1)
                assertThat(
                    connection.count(
                        "SELECT COUNT(*) FROM group_wallet_item WHERE wallet_item_id = '${fixture.departingWalletItemId}'",
                    ),
                ).isZero()
                assertThat(
                    connection.count(
                        "SELECT COUNT(*) FROM group_wallet_item WHERE wallet_item_id = '${fixture.ownerWalletItemId}'",
                    ),
                ).isEqualTo(1)

                assertRecurrenceActive(connection, fixture.createdByDepartingOnOwnerItemRecurrenceId)
                assertRecurrenceEnded(connection, fixture.createdByOwnerOnDepartingItemRecurrenceId)
                assertRecurrenceEnded(connection, fixture.spanningMembersRecurrenceId)
                assertScheduleActive(connection, fixture.ownerGoalScheduleId)
                assertScheduleEnded(connection, fixture.departingGoalScheduleId)

                assertThat(connection.count("SELECT COUNT(*) FROM wallet_event WHERE id = '${fixture.walletEventId}'"))
                    .isEqualTo(1)
                assertThat(connection.count("SELECT COUNT(*) FROM wallet_entry WHERE id = '${fixture.walletEntryId}'"))
                    .isEqualTo(1)
                assertThat(
                    connection.uuid(
                        "SELECT created_by_user_id FROM wallet_event WHERE id = '${fixture.walletEventId}'",
                    ),
                ).isEqualTo(fixture.departingUserId)
                assertThat(
                    connection.decimal(
                        "SELECT balance FROM group_member_debt_monthly WHERE id = '${fixture.debtMonthlyId}'",
                    ),
                ).isEqualByComparingTo("37.50")
                assertThat(
                    connection.uuid(
                        "SELECT created_by_user_id FROM group_member_debt_movement " +
                            "WHERE id = '${fixture.debtMovementId}'",
                    ),
                ).isEqualTo(fixture.departingUserId)
            }

            val overview =
                webClient
                    .get()
                    .uri("/groups/${fixture.groupId}/dashboard/overview?month=2026-08")
                    .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(GroupOverviewDashboardDto::class.java)
                    .returnResult()
                    .responseBody!!

            assertThat(overview.debtPairs).hasSize(1)
            assertThat(overview.debtPairs.single().payerId).isEqualTo(fixture.departingUserId)
            assertThat(overview.debtPairs.single().payerName).isNull()
            assertThat(overview.debtPairs.single().receiverId).isEqualTo(fixture.ownerUserId)
            assertThat(overview.debtPairs.single().receiverName).isNotBlank()
            assertThat(overview.debtPairs.single().outstandingAmount).isEqualByComparingTo("37.50")
            Unit
        }

    @Test
    fun `queued recurrence and group mutations cannot revive departed wallet items`() =
        runBlocking {
            val fixture = seedFixture()
            val queuedExecutionDate = LocalDate.of(2026, 8, 10)

            assertThat(groupService.leaveGroup(fixture.departingUserId, fixture.groupId)).isTrue()

            val generated =
                walletEntryCreateService.createFromRecurrenceConfig(
                    recurrenceConfigId = fixture.queuedRecurrenceId,
                    date = queuedExecutionDate,
                    confirmedOverride = null,
                )

            assertThat(generated).isNull()
            openConnection().use { connection ->
                assertThat(
                    connection.count(
                        "SELECT COUNT(*) FROM wallet_event WHERE recurrence_event_id = '${fixture.queuedRecurrenceId}'",
                    ),
                ).isZero()
                assertRecurrenceEnded(connection, fixture.queuedRecurrenceId)
            }

            val blockedEntry = entryRequest(fixture, "Blocked new expense", queuedExecutionDate)
            webClient
                .post()
                .uri("/wallet-entries")
                .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(blockedEntry)
                .exchange()
                .expectStatus()
                .isNoContent

            webClient
                .put()
                .uri("/wallet-entries/${fixture.walletEventId}")
                .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(blockedEntry.copy(name = "Blocked historical edit"))
                .exchange()
                .expectStatus()
                .isNoContent

            webClient
                .put()
                .uri("/wallet-entries/scheduled/${fixture.createdByOwnerOnDepartingItemRecurrenceId}")
                .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    EditScheduledEntryDto(
                        occurrenceDate = queuedExecutionDate,
                        scope = ScheduledEditScope.THIS_AND_FUTURE,
                        entry =
                            blockedEntry.copy(
                                name = "Blocked schedule resume",
                                paymentType = PaymentType.RECURRING,
                                periodicity = RecurrenceType.MONTHLY,
                                periodicityQtyLimit = 10,
                            ),
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent

            openConnection().use { connection ->
                assertThat(
                    connection.count("SELECT COUNT(*) FROM wallet_event WHERE name = 'Blocked new expense'"),
                ).isZero()
                assertThat(connection.text("SELECT name FROM wallet_event WHERE id = '${fixture.walletEventId}'"))
                    .isEqualTo("Historical expense")
                assertRecurrenceEnded(connection, fixture.createdByOwnerOnDepartingItemRecurrenceId)
                assertThat(
                    connection.text(
                        "SELECT name FROM recurrence_event " +
                            "WHERE id = '${fixture.createdByOwnerOnDepartingItemRecurrenceId}'",
                    ),
                ).isEqualTo("Owner on departing item")
            }
            Unit
        }

    private fun seedFixture(): Fixture {
        val owner = ownerUtil.createUserOnDatabase()
        val departing = departingUtil.createUserOnDatabase()
        val ownerToken = ownerUtil.login()
        val fixture = Fixture(owner.id!!, departing.id!!, ownerToken)

        openConnection().use { connection ->
            connection.autoCommit = false
            connection.exec(
                """
                INSERT INTO "group"(id, name, owner_user_id)
                VALUES ('${fixture.groupId}', 'Departure integration', '${fixture.ownerUserId}')
                """.trimIndent(),
            )
            connection.exec(
                """
                INSERT INTO group_user(id, group_id, user_id, role)
                VALUES
                    ('${UUID.randomUUID()}', '${fixture.groupId}', '${fixture.ownerUserId}', 'ADMIN'),
                    ('${UUID.randomUUID()}', '${fixture.groupId}', '${fixture.departingUserId}', 'EDITOR')
                """.trimIndent(),
            )
            seedWalletItems(connection, fixture)
            seedRecurrences(connection, fixture)
            seedGoalSchedules(connection, fixture)
            seedHistoryAndDebt(connection, fixture)
            connection.commit()
        }
        return fixture
    }

    private fun seedWalletItems(
        connection: Connection,
        fixture: Fixture,
    ) {
        connection.exec(
            """
            INSERT INTO wallet_item(id, user_id, type, name, currency, enabled, balance)
            VALUES
                ('${fixture.ownerWalletItemId}', '${fixture.ownerUserId}', 'BANK_ACCOUNT', 'Owner account', 'BRL', true, 500.00),
                ('${fixture.departingWalletItemId}', '${fixture.departingUserId}', 'BANK_ACCOUNT', 'Departing account', 'BRL', true, 250.00)
            """.trimIndent(),
        )
        connection.exec(
            """
            INSERT INTO group_wallet_item(id, group_id, wallet_item_id)
            VALUES
                ('${UUID.randomUUID()}', '${fixture.groupId}', '${fixture.ownerWalletItemId}'),
                ('${UUID.randomUUID()}', '${fixture.groupId}', '${fixture.departingWalletItemId}')
            """.trimIndent(),
        )
    }

    private fun seedRecurrences(
        connection: Connection,
        fixture: Fixture,
    ) {
        listOf(
            fixture.createdByDepartingOnOwnerItemRecurrenceId,
            fixture.createdByOwnerOnDepartingItemRecurrenceId,
            fixture.spanningMembersRecurrenceId,
            fixture.queuedRecurrenceId,
        ).forEach { recurrenceId ->
            connection.exec("INSERT INTO recurrence_series(id, qty_total) VALUES ('$recurrenceId', 10)")
        }

        insertRecurrence(
            connection,
            fixture.createdByDepartingOnOwnerItemRecurrenceId,
            fixture.departingUserId,
            fixture.groupId,
            "Departing author on owner item",
        )
        insertRecurrence(
            connection,
            fixture.createdByOwnerOnDepartingItemRecurrenceId,
            fixture.ownerUserId,
            fixture.groupId,
            "Owner on departing item",
        )
        insertRecurrence(
            connection,
            fixture.spanningMembersRecurrenceId,
            fixture.ownerUserId,
            fixture.groupId,
            "Spanning members",
        )
        insertRecurrence(
            connection,
            fixture.queuedRecurrenceId,
            fixture.ownerUserId,
            fixture.groupId,
            "Already queued",
        )

        insertRecurrenceEntry(
            connection,
            fixture.createdByDepartingOnOwnerItemRecurrenceId,
            fixture.ownerWalletItemId,
        )
        insertRecurrenceEntry(
            connection,
            fixture.createdByOwnerOnDepartingItemRecurrenceId,
            fixture.departingWalletItemId,
        )
        insertRecurrenceEntry(connection, fixture.spanningMembersRecurrenceId, fixture.ownerWalletItemId)
        insertRecurrenceEntry(connection, fixture.spanningMembersRecurrenceId, fixture.departingWalletItemId)
        insertRecurrenceEntry(connection, fixture.queuedRecurrenceId, fixture.departingWalletItemId)
    }

    private fun insertRecurrence(
        connection: Connection,
        recurrenceId: UUID,
        createdByUserId: UUID,
        groupId: UUID,
        name: String,
    ) {
        connection.exec(
            """
            INSERT INTO recurrence_event(
                id, type, payment_type, periodicity, name, created_by_user_id, group_id,
                qty_executed, qty_limit, last_execution, next_execution, end_execution,
                series_id, series_offset
            ) VALUES (
                '$recurrenceId', 'EXPENSE', 'RECURRING', 'MONTHLY', '$name', '$createdByUserId', '$groupId',
                2, 10, DATE '2026-07-10', DATE '2026-08-10', DATE '2027-03-10',
                '$recurrenceId', 0
            )
            """.trimIndent(),
        )
    }

    private fun insertRecurrenceEntry(
        connection: Connection,
        recurrenceId: UUID,
        walletItemId: UUID,
    ) {
        connection.exec(
            """
            INSERT INTO recurrence_entry(
                id, wallet_event_id, wallet_item_id, value, contribution_percent
            ) VALUES (
                '${UUID.randomUUID()}', '$recurrenceId', '$walletItemId', -25.00, 100.00
            )
            """.trimIndent(),
        )
    }

    private fun seedGoalSchedules(
        connection: Connection,
        fixture: Fixture,
    ) {
        connection.exec(
            """
            INSERT INTO financial_goal(id, name, group_id, deadline)
            VALUES ('${fixture.goalId}', 'Shared reserve', '${fixture.groupId}', DATE '2027-08-01')
            """.trimIndent(),
        )
        connection.exec(
            """
            INSERT INTO financial_goal_contribution_schedule(
                id, financial_goal_id, wallet_item_id, amount, currency, periodicity,
                qty_executed, qty_limit, last_execution, next_execution, end_execution
            ) VALUES
                (
                    '${fixture.ownerGoalScheduleId}', '${fixture.goalId}', '${fixture.ownerWalletItemId}',
                    20.00, 'BRL', 'MONTHLY', 2, 10, DATE '2026-07-10', DATE '2026-08-10', DATE '2027-03-10'
                ),
                (
                    '${fixture.departingGoalScheduleId}', '${fixture.goalId}', '${fixture.departingWalletItemId}',
                    30.00, 'BRL', 'MONTHLY', 2, 10, DATE '2026-07-10', DATE '2026-08-10', DATE '2027-03-10'
                )
            """.trimIndent(),
        )
    }

    private fun seedHistoryAndDebt(
        connection: Connection,
        fixture: Fixture,
    ) {
        connection.exec(
            """
            INSERT INTO wallet_event(
                id, type, payment_type, name, created_by_user_id, group_id, date, confirmed
            ) VALUES (
                '${fixture.walletEventId}', 'EXPENSE', 'UNIQUE', 'Historical expense',
                '${fixture.departingUserId}', '${fixture.groupId}', DATE '2026-08-01', true
            )
            """.trimIndent(),
        )
        connection.exec(
            """
            INSERT INTO wallet_entry(id, value, wallet_event_id, wallet_item_id, contribution_percent)
            VALUES (
                '${fixture.walletEntryId}', -37.50, '${fixture.walletEventId}', '${fixture.departingWalletItemId}', 100.00
            )
            """.trimIndent(),
        )
        connection.exec(
            """
            INSERT INTO group_member_debt_monthly(
                id, group_id, payer_id, receiver_id, month, currency, balance
            ) VALUES (
                '${fixture.debtMonthlyId}', '${fixture.groupId}', '${fixture.departingUserId}',
                '${fixture.ownerUserId}', DATE '2026-08-01', 'BRL', 37.50
            )
            """.trimIndent(),
        )
        connection.exec(
            """
            INSERT INTO group_member_debt_movement(
                id, group_id, payer_id, receiver_id, month, currency, delta_signed,
                reason_kind, created_by_user_id, source_wallet_event_id, note
            ) VALUES (
                '${fixture.debtMovementId}', '${fixture.groupId}', '${fixture.departingUserId}',
                '${fixture.ownerUserId}', DATE '2026-08-01', 'BRL', 37.50,
                'BENEFICIARY_CHARGE', '${fixture.departingUserId}', '${fixture.walletEventId}', 'seed'
            )
            """.trimIndent(),
        )
    }

    private fun entryRequest(
        fixture: Fixture,
        name: String,
        date: LocalDate,
    ) = NewEntryDto(
        type = WalletEntryType.EXPENSE,
        groupId = fixture.groupId,
        sources =
            listOf(
                WalletSourceLegDto(
                    walletItemId = fixture.departingWalletItemId,
                    contributionPercent = BigDecimal("100.00"),
                ),
            ),
        beneficiaries =
            listOf(
                WalletBeneficiaryLegDto(
                    userId = fixture.ownerUserId,
                    benefitPercent = BigDecimal("100.00"),
                ),
            ),
        targetId = null,
        name = name,
        categoryId = null,
        date = date,
        value = BigDecimal("25.00"),
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
    )

    private fun assertRecurrenceActive(
        connection: Connection,
        recurrenceId: UUID,
    ) {
        assertThat(connection.date("SELECT next_execution FROM recurrence_event WHERE id = '$recurrenceId'"))
            .isEqualTo(LocalDate.of(2026, 8, 10))
        assertThat(connection.integer("SELECT qty_limit FROM recurrence_event WHERE id = '$recurrenceId'"))
            .isEqualTo(10)
    }

    private fun assertRecurrenceEnded(
        connection: Connection,
        recurrenceId: UUID,
    ) {
        assertThat(connection.dateOrNull("SELECT next_execution FROM recurrence_event WHERE id = '$recurrenceId'"))
            .isNull()
        assertThat(connection.date("SELECT end_execution FROM recurrence_event WHERE id = '$recurrenceId'"))
            .isEqualTo(LocalDate.of(2026, 7, 10))
        assertThat(connection.integer("SELECT qty_limit FROM recurrence_event WHERE id = '$recurrenceId'"))
            .isEqualTo(2)
    }

    private fun assertScheduleActive(
        connection: Connection,
        scheduleId: UUID,
    ) {
        assertThat(
            connection.date(
                "SELECT next_execution FROM financial_goal_contribution_schedule WHERE id = '$scheduleId'",
            ),
        ).isEqualTo(LocalDate.of(2026, 8, 10))
        assertThat(
            connection.integer(
                "SELECT qty_limit FROM financial_goal_contribution_schedule WHERE id = '$scheduleId'",
            ),
        ).isEqualTo(10)
    }

    private fun assertScheduleEnded(
        connection: Connection,
        scheduleId: UUID,
    ) {
        assertThat(
            connection.dateOrNull(
                "SELECT next_execution FROM financial_goal_contribution_schedule WHERE id = '$scheduleId'",
            ),
        ).isNull()
        assertThat(
            connection.date(
                "SELECT end_execution FROM financial_goal_contribution_schedule WHERE id = '$scheduleId'",
            ),
        ).isEqualTo(LocalDate.of(2026, 7, 10))
        assertThat(
            connection.integer(
                "SELECT qty_limit FROM financial_goal_contribution_schedule WHERE id = '$scheduleId'",
            ),
        ).isEqualTo(2)
    }

    private fun openConnection(): Connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun Connection.exec(sql: String) {
        createStatement().use { statement -> statement.execute(sql) }
    }

    private fun Connection.count(sql: String): Long = query(sql) { it.getLong(1) }

    private fun Connection.uuid(sql: String): UUID = query(sql) { it.getObject(1, UUID::class.java) }

    private fun Connection.decimal(sql: String): BigDecimal = query(sql) { it.getBigDecimal(1) }

    private fun Connection.text(sql: String): String = query(sql) { it.getString(1) }

    private fun Connection.integer(sql: String): Int = query(sql) { it.getInt(1) }

    private fun Connection.date(sql: String): LocalDate = query(sql) { it.getObject(1, LocalDate::class.java) }

    private fun Connection.dateOrNull(sql: String): LocalDate? = query(sql) { it.getObject(1, LocalDate::class.java) }

    private fun <T> Connection.query(
        sql: String,
        mapper: (java.sql.ResultSet) -> T,
    ): T =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { result ->
                check(result.next())
                mapper(result)
            }
        }

    private data class Fixture(
        val ownerUserId: UUID,
        val departingUserId: UUID,
        val ownerToken: String,
        val groupId: UUID = UUID.randomUUID(),
        val ownerWalletItemId: UUID = UUID.randomUUID(),
        val departingWalletItemId: UUID = UUID.randomUUID(),
        val createdByDepartingOnOwnerItemRecurrenceId: UUID = UUID.randomUUID(),
        val createdByOwnerOnDepartingItemRecurrenceId: UUID = UUID.randomUUID(),
        val spanningMembersRecurrenceId: UUID = UUID.randomUUID(),
        val queuedRecurrenceId: UUID = UUID.randomUUID(),
        val goalId: UUID = UUID.randomUUID(),
        val ownerGoalScheduleId: UUID = UUID.randomUUID(),
        val departingGoalScheduleId: UUID = UUID.randomUUID(),
        val walletEventId: UUID = UUID.randomUUID(),
        val walletEntryId: UUID = UUID.randomUUID(),
        val debtMonthlyId: UUID = UUID.randomUUID(),
        val debtMovementId: UUID = UUID.randomUUID(),
    )
}
