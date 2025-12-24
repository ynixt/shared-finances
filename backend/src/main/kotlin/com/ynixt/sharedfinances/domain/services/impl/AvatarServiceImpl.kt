package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.services.AvatarService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.UUID

@Service
class AvatarServiceImpl(
    webClientBuilder: WebClient.Builder,
    private val s3: S3AsyncClient,
    @param:Value("\${app.s3.bucket}") private val bucket: String,
) : AvatarService {
    private val gravatarClient: WebClient =
        webClientBuilder
            .baseUrl("https://www.gravatar.com")
            .build()

    override fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): Mono<String> {
        val normalized = email.trim().lowercase(Locale.ROOT)
        val md5Hex = md5Hex(normalized)

        val size = 256
        val rating = "g"
        val gravatarPath = "/avatar/$md5Hex?s=$size&d=404&r=$rating"

        val objectKey = "avatar/$userId"

        return downloadGravatar(gravatarPath)
            .flatMap { bytesAndType ->
                putObject(
                    bucket = bucket,
                    key = objectKey,
                    bytes = bytesAndType.bytes,
                    contentType = bytesAndType.contentType ?: "application/octet-stream",
                )
            }.map { "/private/external/$bucket/$objectKey" }
    }

    private fun downloadGravatar(path: String): Mono<BytesAndType> =
        gravatarClient
            .get()
            .uri(path)
            .exchangeToMono { resp ->
                when (resp.statusCode()) {
                    HttpStatus.NOT_FOUND -> Mono.empty()
                    HttpStatus.OK -> {
                        val contentType =
                            resp
                                .headers()
                                .contentType()
                                .map { it.toString() }
                                .orElse(null)
                        resp
                            .bodyToMono<ByteArray>()
                            .map { bytes -> BytesAndType(bytes, contentType) }
                    }

                    else -> resp.createException().flatMap { Mono.error(it) }
                }
            }.timeout(Duration.ofSeconds(10))

    private fun putObject(
        bucket: String,
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): Mono<Unit> {
        val req =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build()

        return Mono
            .fromFuture(s3.putObject(req, AsyncRequestBody.fromBytes(bytes)))
            .thenReturn(Unit)
    }

    private fun md5Hex(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    private data class BytesAndType(
        val bytes: ByteArray,
        val contentType: String?,
    )
}
