package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.OnlyIdDto
import com.ynixt.sharedfinances.application.web.dto.groups.invite.GroupInfoForInviteDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupInviteDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupInviteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(
    name = "Group invite",
    description = "Operations related to group invites",
)
class GroupInviteController(
    private val groupInviteDtoMapper: GroupInviteDtoMapper,
    private val groupDtoMapper: GroupDtoMapper,
    private val groupInviteService: GroupInviteService,
) {
    @Operation(summary = "Get invite by id")
    @GetMapping("/open/group-invite/{id}")
    suspend fun findOne(
        @PathVariable id: UUID,
    ): ResponseEntity<GroupInfoForInviteDto> =
        groupInviteService
            .findInfoForInvite(
                id,
            ).let { groupInvite ->
                ResponseEntity.ofNullable(groupInvite?.let { groupInviteDtoMapper.toDto(it) })
            }

    @Operation(summary = "Accept the invite, by id, in the logged user")
    @PutMapping("/group-invite/{id}/accept")
    suspend fun accept(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<OnlyIdDto> =
        groupInviteService
            .accept(
                userId = principalToken.principal.id,
                inviteId = id,
            ).let { id ->
                ResponseEntity.ofNullable(id?.let { OnlyIdDto(it) })
            }
}
