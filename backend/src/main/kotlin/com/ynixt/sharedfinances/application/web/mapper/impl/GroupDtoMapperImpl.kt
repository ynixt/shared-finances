package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupWithRoleDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDtoMapper
import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class GroupDtoMapperImpl : GroupDtoMapper {
    override fun toDto(from: Group): GroupDto = GroupToDtoMapper.map(from)

    override fun toDto(from: GroupWithRole): GroupWithRoleDto = GroupWithRoleToDtoMapper.map(from)

    override fun fromDto(from: GroupWithRoleDto): Group = GroupFromDtoMapper.map(from)

    override fun fromNewDtoToNewRequest(from: NewGroupDto): NewGroupRequest = GroupFromNewDtoMapper.map(from)

//    override fun fromEditDtoToEditRequest(from: EditGroupDto): EditGroupRequest = GroupFromEditDtoMapper.map(from)

    private object GroupToDtoMapper : ObjectMappie<Group, GroupDto>() {
        override fun map(from: Group) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object GroupWithRoleToDtoMapper : ObjectMappie<GroupWithRole, GroupWithRoleDto>() {
        override fun map(from: GroupWithRole) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object GroupFromDtoMapper : ObjectMappie<GroupWithRoleDto, Group>() {
        override fun map(from: GroupWithRoleDto) = mapping {}
    }

    private object GroupFromNewDtoMapper : ObjectMappie<NewGroupDto, NewGroupRequest>() {
        override fun map(from: NewGroupDto) = mapping {}
    }

//    private object GroupFromEditDtoMapper : ObjectMappie<EditGroupDto, EditGroupRequest>() {
//        override fun map(from: EditGroupDto) = mapping {}
//    }
}
