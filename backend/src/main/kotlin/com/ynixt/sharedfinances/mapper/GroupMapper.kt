package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.model.dto.group.GroupDto
import com.ynixt.sharedfinances.model.dto.group.GroupViewDto
import com.ynixt.sharedfinances.model.dto.group.GroupWithUserDto
import com.ynixt.sharedfinances.model.dto.group.UpdateGroupDto
import org.mapstruct.Mapper
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface GroupMapper {
    fun toDto(group: Group?): GroupDto?
    fun toDtoList(group: List<Group>?): List<GroupDto>?
    fun update(@MappingTarget group: Group?, updateDto: UpdateGroupDto?)
    fun toViewDto(group: Group?): GroupViewDto?

    fun toGroupWithUserDto(group: Group?): GroupWithUserDto?
    fun toGroupWithUserDtoList(group: List<Group>?): List<GroupWithUserDto>?
}
