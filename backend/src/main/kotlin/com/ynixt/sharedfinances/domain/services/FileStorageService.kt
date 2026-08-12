package com.ynixt.sharedfinances.domain.services

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.Resource

interface FileStorageService {
    suspend fun write(
        key: String,
        bytes: ByteArray,
    )

    suspend fun write(
        key: String,
        chunks: Flow<ByteArray>,
    )

    suspend fun find(key: String): Resource?

    suspend fun delete(key: String): Boolean
}
