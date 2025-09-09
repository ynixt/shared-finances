package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupWithRoleDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest

interface GroupDtoMapper {
    fun toDto(from: GroupWithRole): GroupWithRoleDto

    fun toDto(from: Group): GroupDto

    fun fromDto(from: GroupWithRoleDto): Group

    fun fromNewDtoToNewRequest(from: NewGroupDto): NewGroupRequest

//    fun fromEditDtoToEditRequest(from: EditGroupDto): EditGroupRequest
}
