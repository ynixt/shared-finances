package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.exceptions.http.HeavyFileException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidFileTypeException
import com.ynixt.sharedfinances.domain.services.AvatarService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

private const val MAX_UPLOAD_BYTES = 2 * 1024 * 1024
private const val TARGET_SIZE = 128

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

        val rating = "g"
        val gravatarPath = "/avatar/$md5Hex?s=$TARGET_SIZE&d=404&r=$rating"

        return downloadGravatar(gravatarPath)
            .flatMap { bytesAndType ->
                upload(
                    userId = userId,
                    bytes = bytesAndType.bytes,
                    contentType = bytesAndType.contentType ?: "application/octet-stream",
                )
            }
    }

    override fun deletePhoto(userId: UUID): Mono<Boolean> {
        val request =
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key("avatar/$userId")
                .build()

        return Mono
            .fromFuture(s3.deleteObject(request))
            .map { it.sdkHttpResponse().isSuccessful }
    }

    private fun getExternalUrl(userId: UUID): String = "/private/external/$bucket/${getObjectKey(userId)}"

    private fun getObjectKey(userId: UUID): String = "avatar/$userId"

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

    override fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): Mono<String> {
        val req =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(getObjectKey(userId))
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build()

        return Mono
            .fromFuture(s3.putObject(req, AsyncRequestBody.fromBytes(bytes)))
            .thenReturn(getExternalUrl(userId))
    }

    override fun upload(
        userId: UUID,
        file: FilePart,
    ): Mono<String> {
        val invalidImage =
            InvalidFileTypeException(
                ImageFormat.entries.map { it.toString() }.toList(),
            )

        val declaredLen = file.headers().contentLength
        if (declaredLen > MAX_UPLOAD_BYTES) {
            return Mono.error(HeavyFileException(MAX_UPLOAD_BYTES))
        }

        return DataBufferUtils
            .join(file.content())
            .map { dataBuffer ->
                try {
                    val size = dataBuffer.readableByteCount()
                    if (size > MAX_UPLOAD_BYTES) throw HeavyFileException(MAX_UPLOAD_BYTES)
                    ByteArray(size).also { dataBuffer.read(it) }
                } finally {
                    DataBufferUtils.release(dataBuffer)
                }
            }.map { inputBytes ->
                sniffImageFormat(inputBytes) ?: throw invalidImage

                val img = ImageIO.read(ByteArrayInputStream(inputBytes)) ?: throw invalidImage

                if (img.width <= 0 || img.height <= 0) throw IllegalArgumentException("Invalid image")
                if (img.width > 4000 || img.height > 4000) throw IllegalArgumentException("Too big image")

                val resized = resizeToSquare(img, TARGET_SIZE)

                val baos = ByteArrayOutputStream()
                if (!ImageIO.write(resized, "png", baos)) {
                    throw IllegalStateException("Failure to encode PNG image.")
                }
                baos.toByteArray()
            }.flatMap { sanitizedPngBytes ->
                val req =
                    PutObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(getObjectKey(userId))
                        .contentType("image/png")
                        .contentLength(sanitizedPngBytes.size.toLong())
                        .build()

                val future: CompletableFuture<PutObjectResponse> =
                    s3.putObject(req, AsyncRequestBody.fromBytes(sanitizedPngBytes))

                Mono.fromFuture(future)
            }.thenReturn(getExternalUrl(userId))
    }

    private fun md5Hex(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    private fun sniffImageFormat(bytes: ByteArray): ImageFormat? {
        if (bytes.size < 12) return null

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        val isPng =
            bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() &&
                bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() &&
                bytes[7] == 0x0A.toByte()

        if (isPng) return ImageFormat.PNG

        // JPEG: FF D8 ... FF D9
        val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        if (isJpeg) return ImageFormat.JPEG

        // WebP: "RIFF" .... "WEBP"
        val isRiff =
            bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte()

        val isWebp =
            isRiff &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()

        if (isWebp) return ImageFormat.WEBP

        return null
    }

    private fun resizeToSquare(
        src: BufferedImage,
        size: Int,
    ): BufferedImage {
        val srcW = src.width
        val srcH = src.height
        val cropSize = minOf(srcW, srcH)
        val x = (srcW - cropSize) / 2
        val y = (srcH - cropSize) / 2
        val cropped = src.getSubimage(x, y, cropSize, cropSize)

        val scaled: Image = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH)

        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.drawImage(scaled, 0, 0, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private enum class ImageFormat { PNG, JPEG, WEBP }

    private data class BytesAndType(
        val bytes: ByteArray,
        val contentType: String?,
    )
}
