package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.GroupMapper
import com.ynixt.sharedfinances.model.dto.group.GroupDto
import com.ynixt.sharedfinances.model.dto.group.NewGroupDto
import com.ynixt.sharedfinances.model.dto.group.UpdateGroupDto
import com.ynixt.sharedfinances.service.GroupService
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/group")
class GroupController(
    private val groupService: GroupService,
    private val securityService: SecurityService,
    private val groupMapper: GroupMapper
) {
    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<GroupDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(groupMapper.toDto(groupService.getOne(user, id)))
    }

    @PutMapping("{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody updateDto: UpdateGroupDto
    ): GroupDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupMapper.toDto(groupService.updateGroup(user, id, updateDto))!!
    }

    @PostMapping
    fun newCreditCard(authentication: Authentication, @RequestBody newDto: NewGroupDto): GroupDto {
        val user = securityService.authenticationToUser(authentication)!!

        return groupMapper.toDto(groupService.newGroup(user, newDto))!!
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable("id") id: Long) {
        val user = securityService.authenticationToUser(authentication)!!

        return groupService.delete(user, id)
    }
}
