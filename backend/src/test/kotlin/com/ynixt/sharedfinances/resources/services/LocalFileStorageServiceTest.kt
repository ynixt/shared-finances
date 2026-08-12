package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.FileStorageProperties
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalFileStorageServiceTest {
    @field:TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `writes reads replaces and deletes a file`() =
        runTest {
            val service = service()

            service.write("avatars/user.png", byteArrayOf(1, 2, 3))
            assertArrayEquals(
                byteArrayOf(1, 2, 3),
                service.find("avatars/user.png")!!.inputStream.use { it.readAllBytes() },
            )

            service.write("avatars/user.png", byteArrayOf(4, 5))
            assertArrayEquals(
                byteArrayOf(4, 5),
                service.find("avatars/user.png")!!.inputStream.use { it.readAllBytes() },
            )

            assertFalse(service.delete("avatars/missing.png"))
            service.delete("avatars/user.png")
            assertNull(service.find("avatars/user.png"))
        }

    @Test
    fun `keeps a file after the storage service is recreated`() =
        runTest {
            service().write("avatars/user.png", byteArrayOf(1, 2, 3))

            val restartedService = service()

            assertArrayEquals(
                byteArrayOf(1, 2, 3),
                restartedService.find("avatars/user.png")!!.inputStream.use { it.readAllBytes() },
            )
        }

    @Test
    fun `streams a large payload without assembling it in memory`() =
        runTest {
            val service = service()
            val chunkSize = 1024 * 1024
            val chunkCount = 64

            service.write(
                "exports/large.csv",
                flow {
                    repeat(chunkCount) { index ->
                        emit(ByteArray(chunkSize) { (index % 251).toByte() })
                    }
                },
            )

            val stored = temporaryDirectory.resolve("exports/large.csv")
            assertEquals(chunkSize.toLong() * chunkCount, Files.size(stored))
            Files.newInputStream(stored).use { input ->
                repeat(chunkCount) { index ->
                    val chunk = input.readNBytes(chunkSize)
                    assertEquals(chunkSize, chunk.size)
                    assertEquals((index % 251).toByte(), chunk.first())
                    assertEquals((index % 251).toByte(), chunk.last())
                }
            }
        }

    @Test
    fun `rejects keys outside the configured root`() =
        runTest {
            val service = service()

            assertFailsWith<IllegalArgumentException> {
                service.write("../outside.txt", byteArrayOf(1))
            }
            assertFailsWith<IllegalArgumentException> {
                service.find(temporaryDirectory.resolve("absolute.txt").toString())
            }
        }

    @Test
    fun `rejects symbolic links inside storage`() =
        runTest {
            val outside = Files.createDirectory(temporaryDirectory.resolveSibling("${temporaryDirectory.fileName}-outside"))
            val link = temporaryDirectory.resolve("linked")

            try {
                Files.createSymbolicLink(link, outside)
            } catch (_: UnsupportedOperationException) {
                return@runTest
            }

            try {
                val service = service()
                assertFailsWith<IllegalArgumentException> {
                    service.write("linked/file.txt", byteArrayOf(1))
                }
            } finally {
                Files.deleteIfExists(link)
                Files.deleteIfExists(outside)
            }
        }

    @Test
    fun `creates a missing root and verifies it`() {
        val root = temporaryDirectory.resolve("new-root")

        val service = LocalFileStorageService(FileStorageProperties(root.toString()))

        assertNotNull(service)
        assert(Files.isDirectory(root))
    }

    @Test
    fun `rejects a root that is a regular file`() {
        val file = Files.createFile(temporaryDirectory.resolve("not-a-directory"))

        assertThrows(IllegalStateException::class.java) {
            LocalFileStorageService(FileStorageProperties(file.toString()))
        }
    }

    private fun service() = LocalFileStorageService(FileStorageProperties(temporaryDirectory.toString()))
}
