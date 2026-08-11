package com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient

import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.repositories.PlanLimitRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class PlanLimitDatabaseClientRepository(
    private val dbClient: DatabaseClient,
) : PlanLimitRepository {
    override suspend fun findAll(): List<PlanLimitEntity> =
        dbClient
            .sql("SELECT scope, plan_key, limit_key, limit_value FROM plan_limit ORDER BY scope, plan_key, limit_key")
            .map { row, _ ->
                PlanLimitEntity(
                    scope = PlanLimitScope.valueOf(row.get("scope", String::class.java)!!),
                    planKey = row.get("plan_key", String::class.java)!!,
                    limitKey = PlanLimitKey.valueOf(row.get("limit_key", String::class.java)!!),
                    limitValue = row.get("limit_value", Integer::class.java)?.toInt(),
                )
            }.all()
            .collectList()
            .awaitSingle()

    override suspend fun upsert(limit: PlanLimitEntity): PlanLimitEntity {
        var spec =
            dbClient
                .sql(
                    """
                    INSERT INTO plan_limit(scope, plan_key, limit_key, limit_value)
                    VALUES (:scope, :planKey, :limitKey, :limitValue)
                    ON CONFLICT (scope, plan_key, limit_key)
                    DO UPDATE SET limit_value = EXCLUDED.limit_value
                    """.trimIndent(),
                ).bind("scope", limit.scope.name)
                .bind("planKey", limit.planKey)
                .bind("limitKey", limit.limitKey.name)
        spec =
            if (limit.limitValue == null) {
                spec.bindNull("limitValue", Integer::class.java)
            } else {
                spec.bind("limitValue", limit.limitValue)
            }
        spec.fetch().rowsUpdated().awaitSingle()
        return limit
    }

    override suspend fun delete(
        scope: PlanLimitScope,
        plan: UserPlanRole,
        quota: PlanLimitKey,
    ) {
        dbClient
            .sql("DELETE FROM plan_limit WHERE scope = :scope AND plan_key = :planKey AND limit_key = :limitKey")
            .bind("scope", scope.name)
            .bind("planKey", plan.name)
            .bind("limitKey", quota.name)
            .fetch()
            .rowsUpdated()
            .awaitFirstOrNull()
    }
}
