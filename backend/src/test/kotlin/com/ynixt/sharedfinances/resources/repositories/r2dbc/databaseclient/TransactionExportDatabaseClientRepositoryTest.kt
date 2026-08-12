package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportCursor
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportFilter
import com.ynixt.sharedfinances.domain.models.exports.TransactionExportRow
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.r2dbc.core.DatabaseClient
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionExportDatabaseClientRepositoryTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `combines active dimensions with AND while values within each dimension use arrays`() {
        val walletIds = setOf(UUID.randomUUID(), UUID.randomUUID())
        val categoryIds = setOf(UUID.randomUUID(), UUID.randomUUID())
        val query =
            repository().filteredQuery(
                userId,
                TransactionExportFilter(
                    dateFrom = LocalDate.of(2026, 1, 1),
                    dateTo = LocalDate.of(2026, 1, 31),
                    walletItemIds = walletIds,
                    categoryIds = categoryIds,
                    entryTypes = setOf(WalletEntryType.REVENUE, WalletEntryType.TRANSFER),
                    tags = setOf("home", "food"),
                    confirmed = true,
                ),
            )

        assertTrue(query.sql.contains("event.date >= :dateFrom AND event.date <= :dateTo"))
        assertTrue(query.sql.contains("entry.wallet_item_id = ANY"))
        assertTrue(query.sql.contains("event.category_id = ANY"))
        assertTrue(query.sql.contains("event.type = ANY"))
        assertTrue(query.sql.contains("event.tags &&"))
        assertTrue(query.sql.contains("event.confirmed = :confirmed"))
        assertEquals(walletIds, (query.bindings["walletItemIds"] as Array<*>).toSet())
        assertEquals(categoryIds, (query.bindings["categoryIds"] as Array<*>).toSet())
        assertEquals(setOf("REVENUE", "TRANSFER"), (query.bindings["entryTypes"] as Array<*>).toSet())
    }

    @Test
    fun `personal and group scopes are mutually exclusive and category access is constrained`() {
        val categoryId = UUID.randomUUID()
        val personal = repository().filteredQuery(userId, TransactionExportFilter(categoryIds = setOf(categoryId)))
        assertTrue(personal.sql.contains("event.created_by_user_id = :userId"))
        assertFalse(personal.sql.contains("event.group_id = :groupId"))
        assertTrue(personal.sql.contains("SELECT group_id FROM group_user WHERE user_id = :userId"))

        val groupId = UUID.randomUUID()
        val group = repository().filteredQuery(userId, TransactionExportFilter(groupId = groupId))
        assertTrue(group.sql.contains("event.group_id = :groupId"))
        assertFalse(group.sql.contains("event.created_by_user_id = :userId"))
        assertEquals(groupId, group.bindings["groupId"])
    }

    @Test
    fun `bill mode compares bill rows by truncated month and other rows by transaction date`() {
        val query =
            repository().filteredQuery(
                userId,
                TransactionExportFilter(
                    dateFrom = LocalDate.of(2026, 1, 15),
                    dateTo = LocalDate.of(2026, 3, 20),
                    billDateMode = true,
                ),
            )

        assertTrue(query.sql.contains("bill.bill_date >= date_trunc('month', CAST(:dateFrom AS DATE))::date"))
        assertTrue(query.sql.contains("bill.bill_date <= date_trunc('month', CAST(:dateTo AS DATE))::date"))
        assertTrue(query.sql.contains("entry.bill_id IS NULL AND event.date >= :dateFrom"))
        assertTrue(query.sql.contains("entry.bill_id IS NULL AND event.date <= :dateTo"))
    }

    @Test
    fun `selection excludes initial balances and can only contain materialized wallet events`() {
        val query = repository().filteredQuery(userId, TransactionExportFilter())

        assertTrue(query.sql.contains("event.initial_balance = FALSE"))
        assertTrue(query.sql.contains("FROM wallet_entry entry"))
        assertTrue(query.sql.contains("JOIN wallet_event event"))
    }

    @Test
    fun `reference columns reuse the existing export join set`() {
        val query = repository().filteredQuery(userId, TransactionExportFilter())
        val joinLines =
            query.sql
                .lineSequence()
                .map(String::trim)
                .filter { it.startsWith("FROM ") || it.startsWith("JOIN ") || it.startsWith("LEFT JOIN ") }
                .toList()

        assertEquals(
            listOf(
                "FROM wallet_entry entry",
                "JOIN wallet_event event ON event.id = entry.wallet_event_id",
                "JOIN wallet_item item ON item.id = entry.wallet_item_id",
                "LEFT JOIN credit_card_bill bill ON bill.id = entry.bill_id",
                "LEFT JOIN wallet_entry_category category ON category.id = event.category_id",
                "LEFT JOIN \"group\" grp ON grp.id = event.group_id",
                "LEFT JOIN recurrence_event recurrence ON recurrence.id = event.recurrence_event_id",
                "LEFT JOIN recurrence_series series ON series.id = recurrence.series_id",
            ),
            joinLines,
        )
    }

    @Test
    fun `non-member group request is rejected before a database query`() =
        runTest {
            val repository = repository(groupAccess = false)

            assertFailsWith<UnauthorizedException> {
                repository.countLines(userId, TransactionExportFilter(groupId = UUID.randomUUID()))
            }
        }

    @Test
    fun `group permission is checked once while the repository streams several pages`() =
        runTest {
            val permission = CountingPermissionService()
            val repository = pagingRepository(permission, pageSizes = listOf(2, 2, 1))

            val rows =
                repository
                    .findRows(
                        userId,
                        TransactionExportFilter(groupId = UUID.randomUUID()),
                        pageSize = 2,
                    ).toList()

            assertEquals(5, rows.size)
            assertEquals(3, repository.pageCalls)
            assertEquals(1, permission.calls)
        }

    @Test
    fun `row streaming requests pages incrementally`() =
        runTest {
            val repository = pagingRepository(CountingPermissionService(), pageSizes = listOf(2, 2, 2))

            val rows =
                repository
                    .findRows(
                        userId,
                        TransactionExportFilter(groupId = UUID.randomUUID()),
                        pageSize = 2,
                    ).take(3)
                    .toList()

            assertEquals(3, rows.size)
            assertEquals(2, repository.pageCalls)
        }

    private fun repository(groupAccess: Boolean = true) =
        TransactionExportDatabaseClientRepository(
            Mockito.mock(DatabaseClient::class.java),
            FixedPermissionService(groupAccess),
        )

    private class FixedPermissionService(
        private val allowed: Boolean,
    ) : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ) = allowed

        override fun getAllPermissionsForRole(role: UserGroupRole) = emptySet<GroupPermissions>()
    }

    private fun pagingRepository(
        permission: CountingPermissionService,
        pageSizes: List<Int>,
    ): PagingRepository {
        var rowIndex = 0
        val pages = pageSizes.map { size -> List(size) { exportRow(rowIndex++) } }
        return PagingRepository(permission, pages)
    }

    private fun exportRow(index: Int): TransactionExportRow {
        val eventId = UUID.nameUUIDFromBytes("event-$index".toByteArray())
        val entryId = UUID.nameUUIDFromBytes("entry-$index".toByteArray())
        val date = LocalDate.of(2026, 1, 1).plusDays(index.toLong())
        return TransactionExportRow(
            origin = entryId.toString(),
            originName = "Account",
            date = date,
            description = "Row $index",
            value = BigDecimal.ONE,
            currency = "BRL",
            category = null,
            categoryName = null,
            categoryConceptId = null,
            group = UUID.nameUUIDFromBytes("group".toByteArray()).toString(),
            groupName = "Group",
            installment = null,
            beneficiaries = null,
            bill = null,
            tags = emptyList(),
            observations = null,
            confirmed = true,
            transactionId = eventId.toString(),
            transferId = null,
            seriesId = null,
            cursor = TransactionExportCursor(date, eventId, entryId),
        )
    }

    private class PagingRepository(
        permission: GroupPermissionService,
        private val pages: List<List<TransactionExportRow>>,
    ) : TransactionExportDatabaseClientRepository(
            Mockito.mock(DatabaseClient::class.java),
            permission,
        ) {
        var pageCalls = 0

        override fun findPage(
            userId: UUID,
            filter: TransactionExportFilter,
            cursor: TransactionExportCursor?,
            pageSize: Int,
        ): Flow<TransactionExportRow> = flowOf(*pages[pageCalls++].toTypedArray())
    }

    private class CountingPermissionService : GroupPermissionService {
        var calls = 0

        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean {
            calls++
            return true
        }

        override fun getAllPermissionsForRole(role: UserGroupRole) = emptySet<GroupPermissions>()
    }
}
