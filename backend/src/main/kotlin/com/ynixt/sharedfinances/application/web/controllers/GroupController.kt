package com.ynixt.sharedfinances.application.web.controllers

import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupUserDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.GroupUserDtoMapper
import com.ynixt.sharedfinances.domain.extensions.MonoExtensions.mapList
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): Mono<List<GroupDto>> =
        groupService
            .findAllGroups(
                principalToken.principal.id,
            ).mapList(groupDtoMapper::toDto)

    @GetMapping("/{id}")
    fun findOne(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable id: UUID,
    ): Mono<ResponseEntity<GroupDto>> =
        groupService
            .findGroup(
                userId = principalToken.principal.id,
                id = id,
            ).map {
                ResponseEntity.ofNullable(groupDtoMapper.toDto(it))
            }.defaultIfEmpty(ResponseEntity.notFound().build())

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
