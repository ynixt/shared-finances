package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.GroupInvite
import com.ynixt.sharedfinances.model.dto.groupinvite.GroupInviteDto
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface GroupInviteMapper {
    fun toDto(groupInvite: GroupInvite?): GroupInviteDto?
}
