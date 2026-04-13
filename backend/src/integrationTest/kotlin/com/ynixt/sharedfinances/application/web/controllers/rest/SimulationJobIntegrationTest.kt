package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.dto.simulationjobs.CreateSimulationJobRequestDto
import com.ynixt.sharedfinances.application.web.dto.simulationjobs.SimulationJobDto
import com.ynixt.sharedfinances.domain.entities.simulation.SimulationJobEntity
import com.ynixt.sharedfinances.domain.enums.SimulationJobStatus
import com.ynixt.sharedfinances.domain.enums.SimulationJobType
import com.ynixt.sharedfinances.domain.queue.producer.SimulationJobDispatchQueueProducer
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.SimulationJobSpringDataRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.fail

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class SimulationJobIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var simulationJobRepository: SimulationJobSpringDataRepository

    @Autowired
    private lateinit var simulationJobService: SimulationJobService

    @Autowired
    private lateinit var simulationJobDispatchQueueProducer: SimulationJobDispatchQueueProducer

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var userATestUtil: UserTestUtil
    private lateinit var userBTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        userATestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
        userBTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `should enqueue and complete simulation job`() {
        runBlocking {
            userATestUtil.createUserOnDatabase()
            val accessToken = userATestUtil.login()

            val created = createJob(accessToken = accessToken, payload = """{"scenario":"smoke"}""")
            assertThat(created.status).isIn(SimulationJobStatus.QUEUED, SimulationJobStatus.RUNNING)

            val completed =
                awaitJobStatus(
                    accessToken = accessToken,
                    jobId = created.id,
                    expected = SimulationJobStatus.COMPLETED,
                )

            assertThat(completed.resultPayload).contains("\"mode\":\"noop\"")
            assertThat(completed.retries).isEqualTo(1)
        }
    }

    @Test
    fun `should allow owner cancellation and reject non-owner`() {
        runBlocking {
            userATestUtil.createUserOnDatabase()
            userBTestUtil.createUserOnDatabase()

            val ownerToken = userATestUtil.login()
            val otherToken = userBTestUtil.login()

            createJob(accessToken = ownerToken, payload = """{"scenario":"first"}""")
            val queuedJob = createJob(accessToken = ownerToken, payload = """{"scenario":"second"}""")

            webClient
                .post()
                .uri("/simulation-jobs/${queuedJob.id}/cancel")
                .header(HttpHeaders.AUTHORIZATION, otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden

            val cancelled =
                webClient
                    .post()
                    .uri("/simulation-jobs/${queuedJob.id}/cancel")
                    .header(HttpHeaders.AUTHORIZATION, ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(SimulationJobDto::class.java)
                    .returnResult()
                    .responseBody!!

            assertThat(cancelled.status).isEqualTo(SimulationJobStatus.CANCELLED)
            val fetched = getJob(ownerToken, queuedJob.id)
            assertThat(fetched.status).isEqualTo(SimulationJobStatus.CANCELLED)
        }
    }

    @Test
    fun `should process duplicate dispatch idempotently`() {
        runBlocking {
            userATestUtil.createUserOnDatabase()
            val token = userATestUtil.login()

            val created = createJob(accessToken = token, payload = """{"scenario":"duplicate"}""")
            simulationJobDispatchQueueProducer.send(created.id)
            simulationJobDispatchQueueProducer.send(created.id)

            val completed =
                awaitJobStatus(
                    accessToken = token,
                    jobId = created.id,
                    expected = SimulationJobStatus.COMPLETED,
                )

            assertThat(completed.retries).isEqualTo(1)
        }
    }

    @Test
    fun `should reconcile expired running lease`() {
        runBlocking {
            val owner = userATestUtil.createUserOnDatabase()
            val ownerToken = userATestUtil.login()
            val ownerId = owner.id!!
            val now = OffsetDateTime.now()

            val stale =
                simulationJobRepository
                    .save(
                        SimulationJobEntity(
                            ownerUserId = ownerId,
                            ownerGroupId = null,
                            requestedByUserId = ownerId,
                            type = SimulationJobType.PLANNING_SIMULATION,
                            status = SimulationJobStatus.RUNNING,
                            requestPayload = """{"scenario":"stale"}""",
                            resultPayload = null,
                            errorMessage = null,
                            leaseExpiresAt = now.minusMinutes(5),
                            workerId = "stale-worker",
                            startedAt = now.minusMinutes(10),
                            finishedAt = null,
                            cancelledAt = null,
                            retries = 0,
                        ),
                    ).awaitSingle()

            val reconciled = simulationJobService.reconcileExpiredLeases()
            assertThat(reconciled).isGreaterThanOrEqualTo(1)

            simulationJobService.dispatchNextQueuedForOwner(ownerId)
            val completed =
                awaitJobStatus(
                    accessToken = ownerToken,
                    jobId = requireNotNull(stale.id),
                    expected = SimulationJobStatus.COMPLETED,
                )

            assertThat(completed.status).isEqualTo(SimulationJobStatus.COMPLETED)
        }
    }

    @Test
    fun `should create and process simulation job for group scope`() {
        runBlocking {
            userATestUtil.createUserOnDatabase()
            userBTestUtil.createUserOnDatabase()

            val ownerToken = userATestUtil.login()
            val outsiderToken = userBTestUtil.login()
            val group = createGroup(ownerToken, "Ops planning")

            val created = createGroupJob(ownerToken, group.id, payload = """{"scenario":"group"}""")
            assertThat(created.status).isIn(SimulationJobStatus.QUEUED, SimulationJobStatus.RUNNING)

            webClient
                .get()
                .uri("/groups/${group.id}/simulation-jobs/${created.id}")
                .header(HttpHeaders.AUTHORIZATION, outsiderToken)
                .exchange()
                .expectStatus()
                .isForbidden

            val completed =
                awaitGroupJobStatus(
                    accessToken = ownerToken,
                    groupId = group.id,
                    jobId = created.id,
                    expected = SimulationJobStatus.COMPLETED,
                )

            assertThat(completed.status).isEqualTo(SimulationJobStatus.COMPLETED)
            assertThat(completed.retries).isEqualTo(1)
        }
    }

    private suspend fun createJob(
        accessToken: String,
        payload: String?,
    ): SimulationJobDto =
        webClient
            .post()
            .uri("/simulation-jobs")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CreateSimulationJobRequestDto(requestPayload = payload))
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(SimulationJobDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getJob(
        accessToken: String,
        jobId: UUID,
    ): SimulationJobDto =
        webClient
            .get()
            .uri("/simulation-jobs/$jobId")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(SimulationJobDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun createGroup(
        accessToken: String,
        name: String,
    ): GroupDto =
        webClient
            .post()
            .uri("/groups")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(NewGroupDto(name = name, categories = null))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun createGroupJob(
        accessToken: String,
        groupId: UUID,
        payload: String?,
    ): SimulationJobDto =
        webClient
            .post()
            .uri("/groups/$groupId/simulation-jobs")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CreateSimulationJobRequestDto(requestPayload = payload))
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(SimulationJobDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getGroupJob(
        accessToken: String,
        groupId: UUID,
        jobId: UUID,
    ): SimulationJobDto =
        webClient
            .get()
            .uri("/groups/$groupId/simulation-jobs/$jobId")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(SimulationJobDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun awaitJobStatus(
        accessToken: String,
        jobId: UUID,
        expected: SimulationJobStatus,
        timeoutMs: Long = 15000,
    ): SimulationJobDto {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val job = getJob(accessToken, jobId)
            if (job.status == expected) {
                return job
            }
            delay(150)
        }
        fail("Job $jobId did not reach status $expected within ${timeoutMs}ms")
    }

    private suspend fun awaitGroupJobStatus(
        accessToken: String,
        groupId: UUID,
        jobId: UUID,
        expected: SimulationJobStatus,
        timeoutMs: Long = 15000,
    ): SimulationJobDto {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val job = getGroupJob(accessToken, groupId, jobId)
            if (job.status == expected) {
                return job
            }
            delay(150)
        }
        fail("Group job $jobId did not reach status $expected within ${timeoutMs}ms")
    }
}
