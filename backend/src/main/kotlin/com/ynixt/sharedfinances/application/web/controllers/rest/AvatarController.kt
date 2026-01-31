package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.domain.models.Wrapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.AvatarReadService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(
    name = "Avatar",
    description = "Operations related to users avatars",
)
class AvatarController(
    private val avatarReadService: AvatarReadService,
    @param:Value("\${app.s3.bucket}") private val bucket: String,
) {
    @GetMapping("/private/external/{bucket}/avatar/{ownerId}")
    suspend fun getAvatar(
        @PathVariable bucket: String,
        @PathVariable ownerId: UUID,
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): ResponseEntity<Wrapper<String>> {
        if (bucket != this.bucket) return ResponseEntity.notFound().build()

        return avatarReadService
            .getAvatar(
                ownerId = ownerId,
                loggedUserId = principalToken.principal.id,
            ).let { link ->
                ResponseEntity.ofNullable(link?.let { Wrapper(it) })
            }
    }
}
