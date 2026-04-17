package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.groups.ChangeRoleGroupUserRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.EditGroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupUserDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupWithRoleDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.UpdateGroupPlanningSimulatorOptInDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupInviteDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupUserDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupInviteService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/groups")
@Tag(
    name = "Groups",
    description = "Operations related to all groups that logged user has access",
)
class GroupController(
    private val groupDtoMapper: GroupDtoMapper,
    private val groupService: GroupService,
    private val groupUserDtoMapper: GroupUserDtoMapper,
    private val groupInviteDtoMapper: GroupInviteDtoMapper,
    private val groupInviteService: GroupInviteService,
) {
    @Operation(summary = "Get all groups")
    @GetMapping
    suspend fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): List<GroupWithRoleDto> =
        groupService
            .findAllGroups(
                principalToken.principal.id,
            ).map(groupDtoMapper::toDto)

    @Operation(summary = "Search groups by name")
    @GetMapping("/search")
    suspend fun search(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
        @RequestParam(required = false) query: String?,
    ): Page<GroupWithRoleDto> =
        groupService
            .searchGroups(
                userId = principalToken.principal.id,
                pageable = pageable,
                query = query,
            ).map(groupDtoMapper::toDto)

    @Operation(summary = "Get a group by id")
    @GetMapping("/{id}")
    suspend fun findOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<GroupWithRoleDto> =
        groupService
            .findGroup(
                userId = principalToken.principal.id,
                id = id,
            ).let { group ->
                ResponseEntity.ofNullable(group?.let { groupDtoMapper.toDto(it) })
            }

    @Operation(summary = "Edit a group by id")
    @PutMapping("/{id}")
    suspend fun edit(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody body: EditGroupDto,
    ): ResponseEntity<GroupWithRoleDto> =
        groupService
            .editGroup(
                userId = principalToken.principal.id,
                id = id,
                groupDtoMapper.fromEditDtoToEditRequest(body),
            ).let { group ->
                ResponseEntity.ofNullable(group?.let { groupDtoMapper.toDto(it) })
            }

    @Operation(summary = "Delete a group by id")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<Unit> =
        groupService
            .deleteGroup(
                userId = principalToken.principal.id,
                id = id,
            ).let { if (it) ResponseEntity.noContent().build() else ResponseEntity.notFound().build() }

    @Operation(summary = "Create a new group")
    @PostMapping
    suspend fun newGroup(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewGroupDto,
    ): GroupDto =
        groupService
            .newGroup(
                principalToken.principal.id,
                groupDtoMapper.fromNewDtoToNewRequest(body),
            ).let(groupDtoMapper::toDto)

    @Operation(summary = "Change a role of a user inside group")
    @PutMapping("/{id}/members/change-role")
    suspend fun changeMemberRole(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody request: ChangeRoleGroupUserRequestDto,
    ): ResponseEntity<Unit> =
        groupService
            .updateMemberRole(
                userId = principalToken.principal.id,
                id = id,
                memberId = request.memberId,
                newRole = request.role,
            ).let { changed ->
                if (changed) ResponseEntity.noContent().build<Unit>() else ResponseEntity.notFound().build()
            }

    @Operation(summary = "Generate a invitation to allow a user to join a group by id")
    @PostMapping("/{id}/members/generate-invitation")
    suspend fun generateInvitation(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<GroupInviteDto> =
        groupInviteService
            .generate(
                userId = principalToken.principal.id,
                groupId = id,
            ).let { groupInvite -> ResponseEntity.ofNullable(groupInvite?.let { groupInviteDtoMapper.toDto(it) }) }

    @Operation(summary = "Get all users that are in a group, by id")
    @GetMapping("/{id}/members")
    suspend fun findAllMembers(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): ResponseEntity<List<GroupUserDto>> =
        groupService
            .findAllMembers(
                userId = principalToken.principal.id,
                id = id,
            ).let { list ->
                ResponseEntity.ofNullable(list.map(groupUserDtoMapper::toDto))
            }

    @Operation(summary = "Update own planning simulator opt-in in group")
    @PutMapping("/{id}/members/me/planning-simulator-opt-in")
    suspend fun updateOwnPlanningSimulatorOptIn(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
        @RequestBody request: UpdateGroupPlanningSimulatorOptInDto,
    ): ResponseEntity<Unit> =
        groupService
            .updateOwnPlanningSimulatorOptIn(
                userId = principalToken.principal.id,
                id = id,
                allowPlanningSimulator = request.allowPlanningSimulator,
            ).let { updated ->
                if (updated) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
            }
}
