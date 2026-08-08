package com.ynixt.sharedfinances.domain.services

import org.springframework.core.io.Resource

interface FileStorageService {
    suspend fun write(
        key: String,
        bytes: ByteArray,
    )

    suspend fun find(key: String): Resource?

    suspend fun delete(key: String): Boolean
}
