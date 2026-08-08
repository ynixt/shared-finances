package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.AvatarReadService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
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
) {
    @GetMapping("/private/avatars/{ownerId}", produces = [MediaType.IMAGE_PNG_VALUE])
    suspend fun getAvatar(
        @PathVariable ownerId: UUID,
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): ResponseEntity<Resource> =
        avatarReadService
            .getAvatar(
                ownerId = ownerId,
                loggedUserId = principalToken.principal.id,
            ).let { resource ->
                if (resource == null) {
                    ResponseEntity.notFound().build()
                } else {
                    ResponseEntity
                        .ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .cacheControl(CacheControl.noCache())
                        .body(resource)
                }
            }
}
