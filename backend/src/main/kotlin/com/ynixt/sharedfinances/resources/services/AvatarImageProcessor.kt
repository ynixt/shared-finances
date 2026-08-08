package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.exceptions.http.HeavyFileException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidFileTypeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal const val MAX_AVATAR_UPLOAD_BYTES = 2 * 1024 * 1024
internal const val AVATAR_SIZE = 128

@Component
class AvatarImageProcessor {
    suspend fun normalizeToPng(bytes: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            if (bytes.size > MAX_AVATAR_UPLOAD_BYTES) {
                throw HeavyFileException(MAX_AVATAR_UPLOAD_BYTES)
            }

            val invalidImage =
                InvalidFileTypeException(
                    ImageFormat.entries.map { it.toString() },
                )
            sniffImageFormat(bytes) ?: throw invalidImage

            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: throw invalidImage
            if (image.width <= 0 || image.height <= 0) {
                throw IllegalArgumentException("Invalid image")
            }
            if (image.width > 4000 || image.height > 4000) {
                throw IllegalArgumentException("Too big image")
            }

            val resized = resizeToSquare(image, AVATAR_SIZE)
            ByteArrayOutputStream().use { output ->
                if (!ImageIO.write(resized, "png", output)) {
                    throw IllegalStateException("Failure to encode PNG image.")
                }
                output.toByteArray()
            }
        }

    private fun sniffImageFormat(bytes: ByteArray): ImageFormat? {
        if (bytes.size < 12) return null

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

        val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        if (isJpeg) return ImageFormat.JPEG

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
        source: BufferedImage,
        size: Int,
    ): BufferedImage {
        val cropSize = minOf(source.width, source.height)
        val x = (source.width - cropSize) / 2
        val y = (source.height - cropSize) / 2
        val cropped = source.getSubimage(x, y, cropSize, cropSize)
        val scaled: Image = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH)

        return BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).also { output ->
            output.createGraphics().use { graphics ->
                graphics.drawImage(scaled, 0, 0, null)
            }
        }
    }

    private enum class ImageFormat { PNG, JPEG, WEBP }
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
