package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.exceptions.http.HeavyFileException
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.FileStorageService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferLimitException
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.UUID

@Service
class AvatarServiceImpl(
    webClientBuilder: WebClient.Builder,
    private val fileStorageService: FileStorageService,
    private val imageProcessor: AvatarImageProcessor,
    @Value("\${app.gravatar.base-url:https://www.gravatar.com}")
    gravatarBaseUrl: String,
) : AvatarService {
    private val gravatarClient: WebClient =
        webClientBuilder
            .clone()
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(MAX_AVATAR_UPLOAD_BYTES + 1)
            }.baseUrl(gravatarBaseUrl)
            .build()

    override suspend fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): String? {
        val normalized = email.trim().lowercase(Locale.ROOT)
        val path = "/avatar/${md5Hex(normalized)}?s=$AVATAR_SIZE&d=404&r=g"
        val bytes = downloadGravatar(path) ?: return null
        return persist(userId, imageProcessor.normalizeToPng(bytes))
    }

    override suspend fun deletePhoto(userId: UUID): Boolean = fileStorageService.delete(AvatarStorage.key(userId))

    override suspend fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): String = persist(userId, imageProcessor.normalizeToPng(bytes))

    override suspend fun upload(
        userId: UUID,
        file: FilePart,
    ): String {
        val declaredLength = file.headers().contentLength
        if (declaredLength > MAX_AVATAR_UPLOAD_BYTES) {
            throw HeavyFileException(MAX_AVATAR_UPLOAD_BYTES)
        }

        val bytes =
            DataBufferUtils
                .join(file.content(), MAX_AVATAR_UPLOAD_BYTES)
                .map { dataBuffer ->
                    try {
                        ByteArray(dataBuffer.readableByteCount()).also { dataBuffer.read(it) }
                    } finally {
                        DataBufferUtils.release(dataBuffer)
                    }
                }.onErrorMap(DataBufferLimitException::class.java) { exception ->
                    HeavyFileException(MAX_AVATAR_UPLOAD_BYTES, exception)
                }.awaitSingle()

        return persist(userId, imageProcessor.normalizeToPng(bytes))
    }

    private suspend fun persist(
        userId: UUID,
        pngBytes: ByteArray,
    ): String {
        fileStorageService.write(AvatarStorage.key(userId), pngBytes)
        return AvatarStorage.publicRoute(userId)
    }

    private suspend fun downloadGravatar(path: String): ByteArray? =
        gravatarClient
            .get()
            .uri(path)
            .exchangeToMono { response ->
                when (response.statusCode()) {
                    HttpStatus.NOT_FOUND -> Mono.empty()
                    HttpStatus.OK ->
                        response
                            .bodyToMono<ByteArray>()
                            .map { bytes ->
                                if (bytes.size > MAX_AVATAR_UPLOAD_BYTES) {
                                    throw HeavyFileException(MAX_AVATAR_UPLOAD_BYTES)
                                }
                                bytes
                            }

                    else -> response.createException().flatMap { Mono.error(it) }
                }
            }.timeout(Duration.ofSeconds(10))
            .onErrorMap(DataBufferLimitException::class.java) { exception ->
                HeavyFileException(MAX_AVATAR_UPLOAD_BYTES, exception)
            }.awaitSingleOrNull()

    private fun md5Hex(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
