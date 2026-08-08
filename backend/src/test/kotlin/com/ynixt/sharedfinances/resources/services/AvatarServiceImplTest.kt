package com.ynixt.sharedfinances.resources.services

import com.sun.net.httpserver.HttpServer
import com.ynixt.sharedfinances.application.config.FileStorageProperties
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.web.reactive.function.client.WebClient
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

class AvatarServiceImplTest {
    @field:TempDir
    lateinit var storageRoot: Path

    private lateinit var server: HttpServer
    private var gravatarStatus = 200
    private var gravatarBody = byteArrayOf()

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/avatar/") { exchange ->
            exchange.sendResponseHeaders(gravatarStatus, gravatarBody.size.toLong())
            exchange.responseBody.use { it.write(gravatarBody) }
        }
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun `uploads replaces and removes a normalized local avatar`() =
        runTest {
            val userId = UUID.randomUUID()
            val service = service()

            val route = service.upload(userId, png(Color.BLUE), "image/png")
            assertEquals("/api/private/avatars/$userId", route)

            service.upload(userId, png(Color.RED), "image/png")
            val stored = ImageIO.read(storageRoot.resolve("avatars/$userId.png").toFile())
            assertEquals(AVATAR_SIZE, stored.width)
            assertEquals(AVATAR_SIZE, stored.height)

            assertTrue(service.deletePhoto(userId))
            assertFalse(service.deletePhoto(userId))
        }

    @Test
    fun `downloads normalizes and stores a gravatar`() =
        runTest {
            val userId = UUID.randomUUID()
            gravatarBody = png(Color.GREEN)

            val route = service().getPhotoFromGravatar(" User@Example.com ", userId)

            assertEquals("/api/private/avatars/$userId", route)
            val stored = Files.readAllBytes(storageRoot.resolve("avatars/$userId.png"))
            assertEquals(AVATAR_SIZE, ImageIO.read(ByteArrayInputStream(stored)).width)
        }

    @Test
    fun `returns null when gravatar has no image`() =
        runTest {
            gravatarStatus = 404

            assertNull(service().getPhotoFromGravatar("missing@example.com", UUID.randomUUID()))
        }

    private fun service(): AvatarServiceImpl {
        val storage = LocalFileStorageService(FileStorageProperties(storageRoot.toString()))
        return AvatarServiceImpl(
            webClientBuilder = WebClient.builder(),
            fileStorageService = storage,
            imageProcessor = AvatarImageProcessor(),
            gravatarBaseUrl = "http://localhost:${server.address.port}",
        )
    }

    private fun png(color: Color): ByteArray {
        val image = BufferedImage(240, 120, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().use { graphics ->
            graphics.color = color
            graphics.fillRect(0, 0, image.width, image.height)
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
