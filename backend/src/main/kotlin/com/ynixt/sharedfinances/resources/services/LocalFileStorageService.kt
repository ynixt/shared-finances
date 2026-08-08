package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.application.config.FileStorageProperties
import com.ynixt.sharedfinances.domain.services.FileStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

@Service
class LocalFileStorageService(
    properties: FileStorageProperties,
) : FileStorageService {
    private val root: Path = initializeRoot(properties.path)

    override suspend fun write(
        key: String,
        bytes: ByteArray,
    ): Unit =
        withContext(Dispatchers.IO) {
            val destination = resolveKey(key)
            createSafeDirectories(destination.parent)

            val temporary = Files.createTempFile(destination.parent, ".sf-write-", ".tmp")
            try {
                FileChannel
                    .open(
                        temporary,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                    ).use { channel ->
                        val buffer = ByteBuffer.wrap(bytes)
                        while (buffer.hasRemaining()) {
                            channel.write(buffer)
                        }
                        channel.force(true)
                    }

                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }

    override suspend fun find(key: String): Resource? =
        withContext(Dispatchers.IO) {
            val path = resolveKey(key)
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                FileSystemResource(path)
            } else {
                null
            }
        }

    override suspend fun delete(key: String): Boolean =
        withContext(Dispatchers.IO) {
            val path = resolveKey(key)
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                Files.deleteIfExists(path)
            } else {
                false
            }
        }

    private fun resolveKey(key: String): Path {
        require(key.isNotBlank()) { "File storage key must not be blank" }

        val relative = Path.of(key)
        require(!relative.isAbsolute) { "File storage key must be relative" }

        val normalized = relative.normalize()
        require(normalized.nameCount > 0 && normalized.toString() != ".") {
            "File storage key must identify a file"
        }

        val resolved = root.resolve(normalized).normalize()
        require(resolved.startsWith(root) && resolved != root) {
            "File storage key resolves outside the configured root"
        }

        rejectSymbolicLinks(resolved)
        return resolved
    }

    private fun rejectSymbolicLinks(path: Path) {
        var current = root
        root.relativize(path).forEach { segment ->
            current = current.resolve(segment)
            require(!Files.isSymbolicLink(current)) {
                "Symbolic links are not allowed inside file storage"
            }
        }
    }

    private fun createSafeDirectories(directory: Path) {
        var current = root
        root.relativize(directory).forEach { segment ->
            current = current.resolve(segment)
            when {
                Files.exists(current, LinkOption.NOFOLLOW_LINKS) -> {
                    require(!Files.isSymbolicLink(current) && Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                        "File storage path component is not a directory"
                    }
                }

                else -> Files.createDirectory(current)
            }
        }
    }

    private fun initializeRoot(configuredPath: String): Path {
        require(configuredPath.isNotBlank()) { "File storage path must not be blank" }

        val configuredRoot = Path.of(configuredPath).toAbsolutePath().normalize()
        try {
            Files.createDirectories(configuredRoot)
            check(Files.isDirectory(configuredRoot) && Files.isReadable(configuredRoot) && Files.isWritable(configuredRoot)) {
                "File storage path must be a readable and writable directory: $configuredRoot"
            }

            val realRoot = configuredRoot.toRealPath()
            val probe = Files.createTempFile(realRoot, ".sf-startup-", ".tmp")
            try {
                Files.write(probe, byteArrayOf(1), StandardOpenOption.TRUNCATE_EXISTING)
                check(Files.readAllBytes(probe).contentEquals(byteArrayOf(1))) {
                    "File storage startup probe could not be read"
                }
            } finally {
                Files.deleteIfExists(probe)
            }
            return realRoot
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Could not initialize file storage at $configuredRoot",
                exception,
            )
        }
    }
}
