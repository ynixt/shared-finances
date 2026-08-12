package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.application.web.controllers.rest.OpenPlanController
import com.ynixt.sharedfinances.domain.entities.PlanLimitEntity
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.repositories.PlanLimitRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Mono
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlanLimitServiceImplTest {
    @Test
    fun `disabled plan model makes user and group limits unlimited without database or cache lookup`() =
        runTest {
            val repository = FakeRepository(listOf(limit(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS, 1)))
            val cache = MemoryCache()
            val service = service(repository, cache, enabled = false)

            PlanLimitKey.entries.filter { it.scope == PlanLimitScope.USER }.forEach { quota ->
                assertNull(service.resolve(UserPlanRole.USER, quota).value)
            }
            PlanLimitKey.entries.filter { it.scope == PlanLimitScope.GROUP }.forEach { quota ->
                assertNull(service.resolve(GroupPlanTier.COMMON, quota).value)
            }
            assertEquals(0, repository.reads)
            assertEquals(0, cache.reads)
            assertEquals(0, cache.writes)
        }

    @Test
    fun `resolves every group limit and treats a missing row as unlimited`() =
        runTest {
            val rows =
                listOf(
                    PlanLimitEntity(PlanLimitScope.GROUP, GroupPlanTier.COMMON.name, PlanLimitKey.GROUP_CATEGORIES, 50),
                    PlanLimitEntity(PlanLimitScope.GROUP, GroupPlanTier.COMMON.name, PlanLimitKey.GROUP_GOALS, 10),
                    PlanLimitEntity(PlanLimitScope.GROUP, GroupPlanTier.COMMON.name, PlanLimitKey.GROUP_ACTIVE_SCHEDULES, 50),
                    PlanLimitEntity(PlanLimitScope.GROUP, GroupPlanTier.COMMON.name, PlanLimitKey.GROUP_MEMBERS, 4),
                )
            val service = service(FakeRepository(rows), MemoryCache())

            assertEquals(50, service.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_CATEGORIES).value)
            assertEquals(10, service.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_GOALS).value)
            assertEquals(50, service.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_ACTIVE_SCHEDULES).value)
            assertEquals(4, service.resolve(GroupPlanTier.COMMON, PlanLimitKey.GROUP_MEMBERS).value)
            assertNull(service.resolve(GroupPlanTier.PRO, PlanLimitKey.GROUP_MEMBERS).value)
        }

    @Test
    fun `refuses a quota whose declared scope does not match the resolved plan`() =
        runTest {
            val service = service(FakeRepository(emptyList()), MemoryCache())
            assertFailsWith<IllegalArgumentException> { service.resolve(UserPlanRole.USER, PlanLimitKey.GROUP_GOALS) }
            assertFailsWith<IllegalArgumentException> { service.resolve(GroupPlanTier.COMMON, PlanLimitKey.GOALS) }
        }

    @Test
    fun `resolves stored and missing limits and reuses the serialized table`() =
        runTest {
            val repository = FakeRepository(listOf(limit(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS, 10)))
            val cache = MemoryCache()
            val service = service(repository, cache)

            assertEquals(10, service.resolve(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS).value)
            assertNull(service.resolve(UserPlanRole.USER, PlanLimitKey.GOALS).value)
            assertEquals(1, repository.reads)
            assertEquals(1, cache.writes)
        }

    @Test
    fun `administrator is unlimited without database or cache lookup`() =
        runTest {
            val repository = FakeRepository(emptyList())
            val cache = MemoryCache()
            val service = service(repository, cache)

            assertNull(service.resolve(UserPlanRole.ADMINISTRATOR, PlanLimitKey.BANK_ACCOUNTS).value)
            assertNull(service.resolve(UserPlanRole.ADMINISTRATOR, PlanLimitKey.EXPORTS_PER_MONTH).value)
            assertNull(service.resolve(UserPlanRole.ADMINISTRATOR, PlanLimitKey.EXPORT_MAX_LINES).value)
            assertEquals(0, repository.reads)
            assertEquals(0, cache.reads)
        }

    @Test
    fun `stale cache refreshes from the database`() =
        runTest {
            val clock = MutableClock(Instant.parse("2026-08-09T12:00:00Z"))
            val repository = FakeRepository(listOf(limit(UserPlanRole.USER, PlanLimitKey.GOALS, 10)))
            val cache = MemoryCache()
            val service = service(repository, cache, clock)
            assertEquals(10, service.resolve(UserPlanRole.USER, PlanLimitKey.GOALS).value)

            repository.rows = listOf(limit(UserPlanRole.USER, PlanLimitKey.GOALS, 12))
            clock.instant = clock.instant.plus(Duration.ofMinutes(6))

            assertEquals(12, service.resolve(UserPlanRole.USER, PlanLimitKey.GOALS).value)
            assertEquals(2, repository.reads)
        }

    @Test
    fun `unreachable Redis falls through to the database`() =
        runTest {
            val repository = FakeRepository(listOf(limit(UserPlanRole.PRO, PlanLimitKey.CATEGORIES, 1000)))
            val cache = MemoryCache(fail = true)
            val service = service(repository, cache)

            assertEquals(1000, service.resolve(UserPlanRole.PRO, PlanLimitKey.CATEGORIES).value)
            assertEquals(1, repository.reads)
        }

    @Test
    fun `enabling the plan model applies the existing stored limits without changing data`() =
        runTest {
            val repository = FakeRepository(listOf(limit(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS, 1)))
            val cache = MemoryCache()

            assertNull(service(repository, cache, enabled = false).resolve(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS).value)
            assertEquals(1, service(repository, cache, enabled = true).resolve(UserPlanRole.USER, PlanLimitKey.BANK_ACCOUNTS).value)
            assertEquals(1, repository.reads)
        }

    @Test
    fun `saved limit refreshes the cache used by the public comparison without a redeploy`() =
        runTest {
            val repository = FakeRepository(listOf(limit(UserPlanRole.USER, PlanLimitKey.GOALS, 10)))
            val properties = PlanProperties(limitCacheTtl = Duration.ofMinutes(5), enabled = true)
            val service = service(repository, MemoryCache())
            val controller = OpenPlanController(service, properties)

            val initial =
                controller
                    .comparison()
                    .body!!
                    .userPlans
                    .first()
                    .limits
                    .first { it.quota == PlanLimitKey.GOALS }
                    .limit
            assertEquals(10, initial)
            service.save(limit(UserPlanRole.USER, PlanLimitKey.GOALS, 12))
            val changed =
                controller
                    .comparison()
                    .body!!
                    .userPlans
                    .first()
                    .limits
                    .first { it.quota == PlanLimitKey.GOALS }
                    .limit
            assertEquals(12, changed)
        }

    private fun service(
        repository: FakeRepository,
        cache: MemoryCache,
        clock: Clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC),
        enabled: Boolean = true,
    ) = PlanLimitServiceImpl(
        repository = repository,
        redisTemplate = cache.template,
        objectMapper = jacksonObjectMapper(),
        properties = PlanProperties(limitCacheTtl = Duration.ofMinutes(5), enabled = enabled),
        clock = clock,
    )

    private fun limit(
        plan: UserPlanRole,
        quota: PlanLimitKey,
        value: Int?,
    ) = PlanLimitEntity(PlanLimitScope.USER, plan, quota, value)

    private class FakeRepository(
        var rows: List<PlanLimitEntity>,
    ) : PlanLimitRepository {
        var reads = 0

        override suspend fun findAll(): List<PlanLimitEntity> = rows.also { reads++ }

        override suspend fun upsert(limit: PlanLimitEntity): PlanLimitEntity =
            limit.also { updated ->
                rows =
                    rows.filterNot {
                        it.scope == updated.scope && it.planKey == updated.planKey && it.limitKey == updated.limitKey
                    } + updated
            }

        override suspend fun delete(
            scope: PlanLimitScope,
            plan: UserPlanRole,
            quota: PlanLimitKey,
        ) {}
    }

    private class MemoryCache(
        fail: Boolean = false,
    ) {
        val template = Mockito.mock(ReactiveRedisTemplate::class.java) as ReactiveRedisTemplate<String, String>
        private val operations = Mockito.mock(ReactiveValueOperations::class.java) as ReactiveValueOperations<String, String>
        private var value: String? = null
        var reads = 0
        var writes = 0

        init {
            Mockito.`when`(template.opsForValue()).thenReturn(operations)
            Mockito.`when`(operations.get(Mockito.anyString())).thenAnswer {
                reads++
                if (fail) Mono.error<String>(IllegalStateException("redis unavailable")) else Mono.justOrEmpty(value)
            }
            Mockito
                .`when`(operations.set(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration::class.java)))
                .thenAnswer { invocation ->
                    writes++
                    if (fail) {
                        Mono.error<Boolean>(IllegalStateException("redis unavailable"))
                    } else {
                        value = invocation.getArgument(1)
                        Mono.just(true)
                    }
                }
        }
    }

    private class MutableClock(
        var instant: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant
    }
}
