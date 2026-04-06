package com.ynixt.sharedfinances.support

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

abstract class IntegrationTestContainers : IntegrationTest() {
    @Autowired
    lateinit var flyway: Flyway

    companion object {
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(
                DockerImageName.parse("postgres:18.3"),
            ).withDatabaseName(env("POSTGRES_APP_DB", "testdb"))
                .withUsername(env("POSTGRES_APP_USER", "test"))
                .withPassword(env("POSTGRES_APP_PASSWORD", "test"))

        val redis: GenericContainer<*> =
            GenericContainer(
                DockerImageName.parse("redis:8.6.1-alpine"),
            ).withExposedPorts(6379)

        val nats: GenericContainer<*> =
            GenericContainer(
                DockerImageName.parse("nats:2.12.4-alpine"),
            ).withExposedPorts(4222)
                .withCommand("-js")

        init {
            Startables.deepStart(postgres, redis, nats).join()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") { postgres.jdbcUrl.replace("jdbc:", "r2dbc:") }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }

            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }

            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }

            registry.add("app.nats.url") { "nats://${nats.host}:${nats.getMappedPort(4222)}" }
        }

        private fun env(
            key: String,
            default: String,
        ): String = System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
    }

    @BeforeEach
    open fun beforeEach() {
        cleanPostgres()
        cleanRedis()
        cleanNats()
    }

    @AfterEach
    open fun afterEach() {
    }

    open fun cleanPostgres() {
        flyway.clean()
        flyway.migrate()
    }

    open fun cleanRedis() {
        redis.execInContainer("redis-cli", "FLUSHALL")
    }

    open fun cleanNats() {
        nats.execInContainer(
            "sh",
            "-c",
            "nats --server=nats://127.0.0.1:4222 stream ls --json 2>/dev/null || true",
        )
    }
}
