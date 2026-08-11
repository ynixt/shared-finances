package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.repositories.PlanLimitRepository
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant

@Service
class PlanLimitServiceImpl(
    private val repository: PlanLimitRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val properties: PlanProperties,
    private val clock: Clock,
) : PlanLimitService {
    override suspend fun resolve(
        plan: UserPlanRole,
        quota: PlanLimitKey,
    ): ResolvedPlanLimit {
        requireScope(quota, PlanLimitScope.USER)
        if (!properties.enabled || plan == UserPlanRole.ADMINISTRATOR) return ResolvedPlanLimit.unlimited()
        val row = loadTable().firstOrNull { it.scope == PlanLimitScope.USER && it.planKey == plan.name && it.limitKey == quota }
        return row?.limitValue?.let(ResolvedPlanLimit::finite) ?: ResolvedPlanLimit.unlimited()
    }

    override suspend fun resolve(
        tier: GroupPlanTier,
        quota: PlanLimitKey,
    ): ResolvedPlanLimit {
        requireScope(quota, PlanLimitScope.GROUP)
        if (!properties.enabled) return ResolvedPlanLimit.unlimited()
        val row = loadTable().firstOrNull { it.scope == PlanLimitScope.GROUP && it.planKey == tier.name && it.limitKey == quota }
        return row?.limitValue?.let(ResolvedPlanLimit::finite) ?: ResolvedPlanLimit.unlimited()
    }

    override suspend fun save(limit: PlanLimitEntity): PlanLimitEntity = repository.upsert(limit).also { refresh() }

    override suspend fun delete(
        scope: PlanLimitScope,
        plan: UserPlanRole,
        quota: PlanLimitKey,
    ) {
        requireScope(quota, scope)
        repository.delete(scope, plan, quota)
        refresh()
    }

    private fun requireScope(
        quota: PlanLimitKey,
        expected: PlanLimitScope,
    ) {
        require(quota.scope == expected) { "Quota ${quota.name} belongs to ${quota.scope}, not $expected" }
    }

    private suspend fun loadTable(): List<PlanLimitEntity> {
        try {
            redisTemplate.opsForValue().get(CACHE_KEY).awaitSingleOrNull()?.let { json ->
                val cached = objectMapper.readValue(json, CachedPlanLimits::class.java)
                if (cached.cachedAt.plus(properties.limitCacheTtl).isAfter(clock.instant())) return cached.limits
            }
        } catch (error: Exception) {
            logger.warn("Plan limit cache read failed; falling back to Postgres", error)
        }
        return databaseTable()
    }

    private suspend fun refresh() {
        databaseTable()
    }

    private suspend fun databaseTable(): List<PlanLimitEntity> {
        val rows = repository.findAll()
        try {
            val json = objectMapper.writeValueAsString(CachedPlanLimits(clock.instant(), rows))
            redisTemplate.opsForValue().set(CACHE_KEY, json, properties.limitCacheTtl).awaitSingleOrNull()
        } catch (error: Exception) {
            logger.warn("Plan limit cache write failed; continuing with Postgres values", error)
        }
        return rows
    }

    data class CachedPlanLimits(
        val cachedAt: Instant,
        val limits: List<PlanLimitEntity>,
    )

    companion object {
        const val CACHE_KEY = "plan-limits:v1"
        private val logger = LoggerFactory.getLogger(PlanLimitServiceImpl::class.java)
    }
}
