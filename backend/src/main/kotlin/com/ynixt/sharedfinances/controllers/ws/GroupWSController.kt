package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.model.dto.group.GroupDto
import com.ynixt.sharedfinances.model.dto.group.GroupViewDto
import com.ynixt.sharedfinances.service.GroupService
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class GroupWSController(
    private val groupService: GroupService, private val securityService: SecurityService
) {
    @MessageMapping("/group")
    @SendToUser("/queue/group")
    fun listGroups(principal: Principal): List<GroupDto> {
        return groupService.listGroupAsGroupDto(securityService.principalToUser(principal)!!)
    }

    @MessageMapping("/group/{groupId}")
    @SendTo("/topic/group/{groupId}")
    fun viewGroup(principal: Principal, @DestinationVariable groupId: Long): GroupViewDto? {
        return groupService.getOneAsViewDto(securityService.principalToUser(principal)!!, groupId)
    }
}
