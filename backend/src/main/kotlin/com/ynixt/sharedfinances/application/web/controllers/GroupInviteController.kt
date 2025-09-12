package com.ynixt.sharedfinances.application.web.controllers

import com.ynixt.sharedfinances.application.web.dto.OnlyIdDto
import com.ynixt.sharedfinances.application.web.dto.groups.invite.GroupInfoForInviteDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupInviteDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.GroupInviteService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
class GroupInviteController(
    private val groupInviteDtoMapper: GroupInviteDtoMapper,
    private val groupDtoMapper: GroupDtoMapper,
    private val groupInviteService: GroupInviteService,
) {
    @GetMapping("/open/group-invite/{id}")
    fun findOne(
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<GroupInfoForInviteDto>> =
        groupInviteService
            .findInfoForInvite(
                id,
            ).map {
                ResponseEntity.ofNullable(groupInviteDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @PutMapping("/group-invite/{id}/accept")
    fun accept(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<OnlyIdDto>> =
        groupInviteService
            .accept(
                userId = principalToken.principal.id,
                inviteId = id,
            ).map {
                ResponseEntity.ofNullable(OnlyIdDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())
}
