package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.exceptions.http.HeavyFileException
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidFileTypeException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith

class AvatarImageProcessorTest {
    private val processor = AvatarImageProcessor()

    @Test
    fun `normalizes a valid image to a square png`() =
        runTest {
            val normalized = processor.normalizeToPng(png(width = 300, height = 100))
            val image = ImageIO.read(ByteArrayInputStream(normalized))

            assertEquals(AVATAR_SIZE, image.width)
            assertEquals(AVATAR_SIZE, image.height)
            assertEquals(0x89.toByte(), normalized[0])
            assertEquals('P'.code.toByte(), normalized[1])
        }

    @Test
    fun `rejects unsupported content`() =
        runTest {
            assertFailsWith<InvalidFileTypeException> {
                processor.normalizeToPng("not-an-image".toByteArray())
            }
        }

    @Test
    fun `rejects content above the byte limit`() =
        runTest {
            assertFailsWith<HeavyFileException> {
                processor.normalizeToPng(ByteArray(MAX_AVATAR_UPLOAD_BYTES + 1))
            }
        }

    private fun png(
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().use { graphics ->
            graphics.color = Color.BLUE
            graphics.fillRect(0, 0, width, height)
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
