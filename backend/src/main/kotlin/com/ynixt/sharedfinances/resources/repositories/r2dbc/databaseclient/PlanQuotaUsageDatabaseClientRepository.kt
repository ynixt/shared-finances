package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.repositories.PlanQuotaUsageRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class PlanQuotaUsageDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : PlanQuotaUsageRepository {
    override suspend fun acquireTransactionLock(
        userId: UUID,
        quota: PlanLimitKey,
    ) {
        dbClient
            .sql("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
            .bind("lockKey", "$userId:${quota.name}")
            .map { _, _ -> true }
            .one()
            .awaitSingle()
    }

    override suspend fun countUsage(
        userId: UUID,
        quota: PlanLimitKey,
        utcMonthStart: Instant,
    ): Long {
        val sql =
            when (quota) {
                PlanLimitKey.BANK_ACCOUNTS -> "SELECT COUNT(*) AS usage FROM wallet_item WHERE user_id = :userId AND type = 'BANK_ACCOUNT'"
                PlanLimitKey.CREDIT_CARDS -> "SELECT COUNT(*) AS usage FROM wallet_item WHERE user_id = :userId AND type = 'CREDIT_CARD'"
                PlanLimitKey.CATEGORIES ->
                    "SELECT COUNT(*) AS usage FROM wallet_entry_category WHERE user_id = :userId AND group_id IS NULL"
                PlanLimitKey.GOALS -> "SELECT COUNT(*) AS usage FROM financial_goal WHERE user_id = :userId AND group_id IS NULL"
                PlanLimitKey.ACTIVE_SCHEDULES ->
                    "SELECT COUNT(*) AS usage FROM recurrence_event WHERE created_by_user_id = :userId AND group_id IS NULL AND next_execution IS NOT NULL"
                PlanLimitKey.IMPORTS_PER_MONTH ->
                    "SELECT COUNT(*) AS usage FROM import_batch WHERE user_id = :userId AND counted_at >= :monthStart"
                PlanLimitKey.SIMULATIONS_PER_MONTH ->
                    "SELECT COUNT(*) AS usage FROM simulation_job WHERE requested_by_user_id = :userId AND counted_at >= :monthStart"
                PlanLimitKey.OWNED_GROUPS -> "SELECT COUNT(*) AS usage FROM \"group\" WHERE owner_user_id = :userId"
                else -> throw IllegalArgumentException("Quota ${quota.name} is not user scoped")
            }
        var spec = dbClient.sql(sql).bind("userId", userId)
        if (quota.monthly) {
            spec = spec.bind("monthStart", OffsetDateTime.ofInstant(utcMonthStart, ZoneOffset.UTC))
        }
        return spec
            .map { row, _ -> row.get("usage", java.lang.Long::class.java)!!.toLong() }
            .one()
            .awaitSingle()
    }

    override suspend fun acquireGroupTransactionLock(
        groupId: UUID,
        quota: PlanLimitKey,
    ) {
        dbClient
            .sql("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
            .bind("lockKey", "group:$groupId:${quota.name}")
            .map { _, _ -> true }
            .one()
            .awaitSingle()
    }

    override suspend fun countGroupUsage(
        groupId: UUID,
        quota: PlanLimitKey,
        includeOutstandingInvitations: Boolean,
    ): Long {
        val sql =
            when (quota) {
                PlanLimitKey.GROUP_CATEGORIES ->
                    "SELECT COUNT(*) AS usage FROM wallet_entry_category WHERE group_id = :groupId"
                PlanLimitKey.GROUP_GOALS ->
                    "SELECT COUNT(*) AS usage FROM financial_goal WHERE group_id = :groupId"
                PlanLimitKey.GROUP_ACTIVE_SCHEDULES ->
                    "SELECT COUNT(*) AS usage FROM recurrence_event WHERE group_id = :groupId AND next_execution IS NOT NULL"
                PlanLimitKey.GROUP_MEMBERS ->
                    if (includeOutstandingInvitations) {
                        """
                        SELECT
                          (SELECT COUNT(*) FROM group_user WHERE group_id = :groupId) +
                          (SELECT COUNT(*) FROM group_invite WHERE group_id = :groupId AND expire_at > CURRENT_TIMESTAMP)
                          AS usage
                        """.trimIndent()
                    } else {
                        "SELECT COUNT(*) AS usage FROM group_user WHERE group_id = :groupId"
                    }
                else -> throw IllegalArgumentException("Quota ${quota.name} is not group scoped")
            }
        return dbClient
            .sql(sql)
            .bind("groupId", groupId)
            .map { row, _ -> row.get("usage", java.lang.Long::class.java)!!.toLong() }
            .one()
            .awaitSingle()
    }
}
