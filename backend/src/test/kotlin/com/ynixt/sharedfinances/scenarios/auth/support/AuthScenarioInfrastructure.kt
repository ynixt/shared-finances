package com.ynixt.sharedfinances.scenarios.auth.support

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.repositories.FailedLoginRepository
import com.ynixt.sharedfinances.domain.repositories.RefreshTokenRepository
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.mfa.MfaService
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.InMemoryAccountDeletionGroupStore
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.RecordingComplianceSimulationJobService
import com.ynixt.sharedfinances.scenarios.support.MutableScenarioClock
import com.ynixt.sharedfinances.scenarios.support.NoOpAvatarService
import com.ynixt.sharedfinances.scenarios.support.NoOpGroupWalletItemRepository
import com.ynixt.sharedfinances.scenarios.support.ScenarioPasswordEncoder
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetAddress
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

internal class AuthScenarioInfrastructure(
    initialDate: LocalDate,
) {
    val clock = MutableScenarioClock(initialDate)
    val objectMapper: ObjectMapper = jacksonObjectMapper()
    val passwordEncoder: PasswordEncoder = ScenarioPasswordEncoder()
    val avatarService: AvatarService = NoOpAvatarService()
    val mfaService: MfaService = NoOpMfaService
    val jwtEncoder: JwtEncoder = ScenarioJwtEncoder(objectMapper)

    val userRepository = InMemoryUserRepository()
    val walletItemRepository = InMemoryWalletItemRepository()
    val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
    val recurrenceEventRepository = InMemoryRecurrenceEventRepository(walletItemRepository)
    val groupStore = InMemoryAccountDeletionGroupStore()
    val groupWalletItemRepository = NoOpGroupWalletItemRepository()
    val simulationJobService = RecordingComplianceSimulationJobService()

    val authStore = InMemoryAuthRedisStore(clock)
    val sessionRepository: SessionRepository = InMemoryAuthSessionRepository(authStore)
    val refreshTokenRepository: RefreshTokenRepository = InMemoryAuthRefreshTokenRepository(authStore)
    val failedLoginRepository: FailedLoginRepository = InMemoryFailedLoginRepository(clock)
}

internal class InMemoryAuthRedisStore(
    private val clock: Clock,
) {
    private data class SessionRecord(
        val entity: SessionEntity,
        val expiresAt: Instant,
    )

    private data class RefreshRecord(
        val entity: RefreshTokenEntity,
        val expiresAt: Instant,
    )

    private val sessionsById = linkedMapOf<UUID, SessionRecord>()
    private val userSessionsByUserId = linkedMapOf<UUID, LinkedHashSet<UUID>>()
    private val refreshByHash = linkedMapOf<String, RefreshRecord>()
    private val refreshHashesBySessionId = linkedMapOf<UUID, LinkedHashSet<String>>()

    fun findSessionById(id: UUID): SessionEntity? {
        cleanupExpired()
        return sessionsById[id]?.entity?.copyEntity()
    }

    fun saveSession(entity: SessionEntity): SessionEntity {
        cleanupExpired()

        val now = clock.instant()
        val sessionId = entity.id ?: UUID.randomUUID()
        val expiresAt = now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()
        val stored =
            entity.copyEntity().also {
                it.id = sessionId
                if (it.createdAt == null) {
                    it.createdAt = OffsetDateTime.ofInstant(now, clock.zone)
                }
                it.updatedAt = OffsetDateTime.ofInstant(now, clock.zone)
            }

        sessionsById[sessionId] = SessionRecord(entity = stored, expiresAt = expiresAt)
        userSessionsByUserId.getOrPut(stored.userId) { linkedSetOf() }.add(sessionId)
        return stored.copyEntity()
    }

    fun deleteSessionById(id: UUID): Long {
        cleanupExpired()
        val removed = sessionsById.remove(id) ?: return 0L

        userSessionsByUserId[removed.entity.userId]?.let { sessions ->
            sessions.remove(id)
            if (sessions.isEmpty()) {
                userSessionsByUserId.remove(removed.entity.userId)
            }
        }

        val refreshHashes = refreshHashesBySessionId.remove(id).orEmpty()
        refreshHashes.forEach(refreshByHash::remove)

        return 1L
    }

    fun deleteAllSessionsByUserId(userId: UUID): Long {
        cleanupExpired()
        val sessionIds = userSessionsByUserId[userId]?.toList().orEmpty()
        val deleted = sessionIds.sumOf(::deleteSessionById)
        userSessionsByUserId.remove(userId)
        return deleted
    }

    fun saveRefreshToken(entity: RefreshTokenEntity): RefreshTokenEntity {
        cleanupExpired()
        val id = entity.id ?: UUID.randomUUID()
        val tokenHashHex = entity.tokenHash.toHexLower()
        val stored =
            RefreshTokenEntity(
                sessionId = entity.sessionId,
                createdAt = entity.createdAt,
                tokenHash = entity.tokenHash.copyOf(),
                expiresAt = entity.expiresAt,
            ).also { it.id = id }

        refreshByHash[tokenHashHex] = RefreshRecord(entity = stored, expiresAt = stored.expiresAt)
        refreshHashesBySessionId.getOrPut(stored.sessionId) { linkedSetOf() }.add(tokenHashHex)
        return stored.copyEntity()
    }

    fun findRefreshTokenByHashAndExpiresAtAfter(
        tokenHash: ByteArray,
        expiresAt: Instant,
    ): RefreshTokenEntity? {
        cleanupExpired()
        val tokenHashHex = tokenHash.toHexLower()
        val stored = refreshByHash[tokenHashHex]?.entity ?: return null
        if (!stored.expiresAt.isAfter(expiresAt)) {
            return null
        }

        return stored.copyEntity(tokenHash = tokenHash.copyOf())
    }

    fun deleteRefreshTokenByHash(tokenHash: ByteArray): Boolean {
        cleanupExpired()
        val tokenHashHex = tokenHash.toHexLower()
        val removed = refreshByHash.remove(tokenHashHex) ?: return false

        refreshHashesBySessionId[removed.entity.sessionId]?.let { hashes ->
            hashes.remove(tokenHashHex)
            if (hashes.isEmpty()) {
                refreshHashesBySessionId.remove(removed.entity.sessionId)
            }
        }

        return true
    }

    fun hasSessionKey(id: UUID): Boolean {
        cleanupExpired()
        return sessionsById.containsKey(id)
    }

    fun hasUserSessionsKey(userId: UUID): Boolean {
        cleanupExpired()
        return userSessionsByUserId[userId]?.isNotEmpty() == true
    }

    fun hasSessionRefreshIndexKey(sessionId: UUID): Boolean {
        cleanupExpired()
        return refreshHashesBySessionId[sessionId]?.isNotEmpty() == true
    }

    fun userSessionCount(userId: UUID): Long {
        cleanupExpired()
        return userSessionsByUserId[userId]?.size?.toLong() ?: 0L
    }

    fun getSessionTtlSeconds(id: UUID): Long? {
        cleanupExpired()
        val record = sessionsById[id] ?: return null
        return Duration.between(clock.instant(), record.expiresAt).seconds.coerceAtLeast(0L)
    }

    private fun cleanupExpired() {
        val now = clock.instant()

        val expiredSessionIds =
            sessionsById
                .filterValues { record -> !record.expiresAt.isAfter(now) }
                .keys
                .toList()
        expiredSessionIds.forEach(::deleteSessionById)

        val expiredTokenHashes =
            refreshByHash
                .filterValues { record -> !record.expiresAt.isAfter(now) }
                .keys
                .toList()
        expiredTokenHashes.forEach { hashHex ->
            val removed = refreshByHash.remove(hashHex) ?: return@forEach
            refreshHashesBySessionId[removed.entity.sessionId]?.let { hashes ->
                hashes.remove(hashHex)
                if (hashes.isEmpty()) {
                    refreshHashesBySessionId.remove(removed.entity.sessionId)
                }
            }
        }
    }

    private fun SessionEntity.copyEntity(): SessionEntity =
        SessionEntity(
            userId = userId,
            userAgent = userAgent,
            ip = ip?.address?.let { address -> InetAddress.getByAddress(address) },
        ).also { copied ->
            copied.id = id
            copied.createdAt = createdAt
            copied.updatedAt = updatedAt
        }

    private fun RefreshTokenEntity.copyEntity(tokenHash: ByteArray = this.tokenHash.copyOf()): RefreshTokenEntity =
        RefreshTokenEntity(
            sessionId = sessionId,
            createdAt = createdAt,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
        ).also { copied ->
            copied.id = id
        }
}

internal class InMemoryAuthSessionRepository(
    private val authStore: InMemoryAuthRedisStore,
) : SessionRepository {
    override fun deleteAllByUserId(userId: UUID): Mono<Long> = Mono.just(authStore.deleteAllSessionsByUserId(userId))

    override fun findById(id: UUID): Mono<SessionEntity> = Mono.justOrEmpty(authStore.findSessionById(id))

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(authStore.deleteSessionById(id))

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(authStore.hasSessionKey(id))

    override fun <S : SessionEntity> save(entity: S): Mono<S> =
        Mono.fromCallable {
            val saved = authStore.saveSession(entity)
            @Suppress("UNCHECKED_CAST")
            saved as S
        }

    override fun <S : SessionEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).concatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<SessionEntity> = Flux.fromIterable(id).flatMap(::findById)
}

internal class InMemoryAuthRefreshTokenRepository(
    private val authStore: InMemoryAuthRedisStore,
) : RefreshTokenRepository {
    override fun deleteByTokenHash(tokenHash: ByteArray): Mono<Boolean> = Mono.just(authStore.deleteRefreshTokenByHash(tokenHash))

    override fun findByTokenHashAndExpiresAtAfter(
        tokenHash: ByteArray,
        expiresAt: Instant,
    ): Mono<RefreshTokenEntity> = Mono.justOrEmpty(authStore.findRefreshTokenByHashAndExpiresAtAfter(tokenHash, expiresAt))

    override fun findById(id: UUID): Mono<RefreshTokenEntity> = Mono.empty()

    override fun deleteById(id: UUID): Mono<Long> = Mono.just(0L)

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(false)

    override fun <S : RefreshTokenEntity> save(entity: S): Mono<S> =
        Mono.fromCallable {
            val saved = authStore.saveRefreshToken(entity)
            @Suppress("UNCHECKED_CAST")
            saved as S
        }

    override fun <S : RefreshTokenEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).concatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<RefreshTokenEntity> = Flux.empty()
}

internal class InMemoryFailedLoginRepository(
    private val clock: Clock,
) : FailedLoginRepository {
    private data class FailedLoginCounter(
        val fails: Int,
        val expiresAt: Instant,
    )

    private val counters = linkedMapOf<String, FailedLoginCounter>()

    override fun incrementFails(
        ip: String,
        email: String,
        ttlSeconds: Long,
    ): Mono<Void> {
        cleanupExpired()
        val key = buildKey(ip = ip, email = email)
        val existing = counters[key]
        counters[key] =
            if (existing == null) {
                FailedLoginCounter(
                    fails = 1,
                    expiresAt = clock.instant().plusSeconds(ttlSeconds.coerceAtLeast(1L)),
                )
            } else {
                existing.copy(fails = existing.fails + 1)
            }

        return Mono.empty()
    }

    override fun deleteByIpAndEmail(
        ip: String,
        email: String,
    ): Mono<Boolean> {
        cleanupExpired()
        val removed = counters.remove(buildKey(ip = ip, email = email))
        return Mono.just(removed != null)
    }

    override fun getFails(
        ip: String,
        email: String,
    ): Mono<Int> {
        cleanupExpired()
        val fails = counters[buildKey(ip = ip, email = email)]?.fails ?: 0
        return Mono.just(fails)
    }

    private fun cleanupExpired() {
        val now = clock.instant()
        counters.entries.removeIf { (_, value) -> !value.expiresAt.isAfter(now) }
    }

    private fun buildKey(
        ip: String,
        email: String,
    ): String = "$ip|$email"
}

internal object NoOpMfaService : MfaService {
    override fun decryptAndVerify(
        secret: String,
        code: String,
    ): Boolean = false

    override suspend fun generateNewChallenge(
        userId: UUID,
        userAgent: String?,
        ip: InetAddress?,
    ): UUID = UUID.randomUUID()

    override suspend fun verifyChallenge(
        challengeId: UUID,
        code: String,
        ip: InetAddress?,
    ): UserEntity = error("MFA is not part of this auth scenario")
}

internal class ScenarioJwtEncoder(
    private val objectMapper: ObjectMapper,
) : JwtEncoder {
    override fun encode(parameters: JwtEncoderParameters): Jwt {
        val headers = (parameters.jwsHeader?.headers ?: emptyMap()).toMutableMap()
        val claims = parameters.claims.claims.toMap()

        val tokenValue =
            listOf(
                encodeTokenPart(headers),
                encodeTokenPart(claims),
                "scenario-signature",
            ).joinToString(".")

        val issuedAt = parameters.claims.issuedAt ?: Instant.now()
        val expiresAt = parameters.claims.expiresAt ?: issuedAt.plusSeconds(60)

        return Jwt(tokenValue, issuedAt, expiresAt, headers, claims)
    }

    private fun encodeTokenPart(content: Map<String, Any>): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(objectMapper.writeValueAsBytes(content))
}

private fun ByteArray.toHexLower(): String = joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
