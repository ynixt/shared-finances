package com.ynixt.sharedfinances.scenarios.accountdeletion.support

import com.ynixt.sharedfinances.domain.services.AvatarService
import org.springframework.http.codec.multipart.FilePart
import java.util.UUID

internal class RecordingAvatarService : AvatarService {
    val deletedUserIds = mutableListOf<UUID>()

    override suspend fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): String? = null

    override suspend fun deletePhoto(userId: UUID): Boolean {
        deletedUserIds.add(userId)
        return true
    }

    override suspend fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): String = "scenario://avatar/$userId"

    override suspend fun upload(
        userId: UUID,
        file: FilePart,
    ): String = "scenario://avatar/$userId"
}
