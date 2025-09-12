package com.ynixt.sharedfinances.application.web.controllers

import com.ynixt.sharedfinances.application.web.dto.groups.ChangeRoleGroupUserRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.EditGroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupUserDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupWithRoleDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupInviteDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupUserDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapList
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.GroupInviteService
import com.ynixt.sharedfinances.domain.services.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/groups")
class GroupController(
    private val groupDtoMapper: GroupDtoMapper,
    private val groupService: GroupService,
    private val groupUserDtoMapper: GroupUserDtoMapper,
    private val groupInviteDtoMapper: GroupInviteDtoMapper,
    private val groupInviteService: GroupInviteService,
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): Mono<List<GroupWithRoleDto>> =
        groupService
            .findAllGroups(
                principalToken.principal.id,
            ).mapList(groupDtoMapper::toDto)

    @GetMapping("/{id}")
    fun findOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<GroupWithRoleDto>> =
        groupService
            .findGroup(
                userId = principalToken.principal.id,
                id = id,
            ).map {
                ResponseEntity.ofNullable(groupDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @PutMapping("/{id}")
    fun edit(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditGroupDto,
    ): Mono<ResponseEntity<GroupWithRoleDto>> =
        groupService
            .editGroup(
                userId = principalToken.principal.id,
                id = id,
                groupDtoMapper.fromEditDtoToEditRequest(body),
            ).map {
                ResponseEntity.ofNullable(groupDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<Unit>> =
        groupService
            .deleteGroup(
                userId = principalToken.principal.id,
                id = id,
            ).map { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }

    @PostMapping
    fun newBankAccount(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewGroupDto,
    ): Mono<GroupDto> =
        groupService
            .newGroup(
                principalToken.principal.id,
                groupDtoMapper.fromNewDtoToNewRequest(body),
            ).map(groupDtoMapper::toDto)

    @PutMapping("/{id}/members/change-role")
    fun changeMemberRole(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody request: ChangeRoleGroupUserRequestDto,
    ): Mono<ResponseEntity<Unit>> =
        groupService
            .updateMemberRole(
                userId = principalToken.principal.id,
                id = id,
                memberId = request.memberId,
                newRole = request.role,
            ).map { changed ->
                if (changed) ResponseEntity.noContent().build<Unit>() else ResponseEntity.unprocessableEntity().build()
            }.defaultIfEmpty(ResponseEntity.notFound().build())

    @PostMapping("/{id}/members/generate-invitation")
    fun generateInvitation(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<GroupInviteDto>> =
        groupInviteService
            .generate(
                userId = principalToken.principal.id,
                groupId = id,
            ).map { ResponseEntity.ofNullable(groupInviteDtoMapper.toDto(it)) }
            .defaultIfEmpty(ResponseEntity.notFound().build())

    @GetMapping("/{id}/members")
    fun findAllMembers(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<List<GroupUserDto>>> =
        groupService
            .findAllMembers(
                userId = principalToken.principal.id,
                id = id,
            ).map { list ->
                ResponseEntity.ofNullable(list.map(groupUserDtoMapper::toDto))
            }.defaultIfEmpty(ResponseEntity.notFound().build())
}
