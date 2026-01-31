package com.ynixt.sharedfinances.domain.services

import org.springframework.http.codec.multipart.FilePart
import java.util.UUID

interface AvatarService {
    suspend fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): String?

    suspend fun deletePhoto(userId: UUID): Boolean

    suspend fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): String

    suspend fun upload(
        userId: UUID,
        file: FilePart,
    ): String
}
