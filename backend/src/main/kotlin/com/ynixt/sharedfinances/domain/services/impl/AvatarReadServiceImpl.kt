package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AvatarReadService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.UUID

@Service
class AvatarReadServiceImpl(
    private val s3: S3AsyncClient,
    private val userRepository: UserRepository,
    private val presigner: S3Presigner,
    @param:Value("\${app.s3.bucket}") private val bucket: String,
) : AvatarReadService {
    override fun getAvatar(
        ownerId: UUID,
        loggedUserId: UUID,
        expiresIn: Duration,
    ): Mono<String> {
        val hasPermission =
            if (loggedUserId == ownerId) {
                Mono.just(true)
            } else {
                userRepository
                    .findAllUsersInSameGroup(loggedUserId)
                    .collectList()
                    .map { users -> users.find { ownerId == it.id } != null }
            }

        return hasPermission.flatMap { has ->
            if (!has) {
                Mono.empty()
            } else {
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

                val url = presigner.presignGetObject(presignReq).url().toString()
                Mono.just(url)
            }
        }
    }
}
