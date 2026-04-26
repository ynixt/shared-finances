package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.repositories.AvatarPresignedUrlCacheRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AvatarReadService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
class AvatarReadServiceImpl(
    private val userRepository: UserRepository,
    private val presigner: S3Presigner,
    private val avatarPresignedUrlCacheRepository: AvatarPresignedUrlCacheRepository,
    @param:Value("\${app.s3.bucket}") private val bucket: String,
) : AvatarReadService {
    override suspend fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
        expiresIn: Duration,
    ): String? {
        val hasPermission =
            if (loggedUserId == ownerId) {
                true
            } else {
                userRepository
                    .findAllUsersInSameGroup(loggedUserId)
                    .collectList()
                    .map { users -> users.find { ownerId == it.id } != null }
                    .awaitSingle()
            }

        if (!hasPermission) return null

        avatarPresignedUrlCacheRepository
            .findByOwnerId(ownerId)
            .awaitSingleOrNull()
            ?.let { cachedPresignedUrl ->
                return cachedPresignedUrl
            }

        val key = "avatar/$ownerId"

        val getReq =
            GetObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build()

        val presignReq =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(expiresIn)
                .getObjectRequest(getReq)
                .build()

        val presignedUrl = presigner.presignGetObject(presignReq).url().toString()
        val cacheTtl = expiresIn.minusSeconds((expiresIn.seconds * 0.1).toLong())

        if (cacheTtl > Duration.ofSeconds(30)) {
            avatarPresignedUrlCacheRepository
                .save(ownerId = ownerId, presignedUrl = presignedUrl, ttl = cacheTtl)
                .awaitSingleOrNull()
        }

        return presignedUrl
    }
}
