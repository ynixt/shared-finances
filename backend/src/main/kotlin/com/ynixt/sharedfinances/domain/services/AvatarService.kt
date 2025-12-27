package com.ynixt.sharedfinances.domain.services

import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono
import java.util.UUID

interface AvatarService {
    fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): Mono<String>

    fun deletePhoto(userId: UUID): Mono<Boolean>

    fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): Mono<String>

    fun upload(
        userId: UUID,
        file: FilePart,
    ): Mono<String>
}
