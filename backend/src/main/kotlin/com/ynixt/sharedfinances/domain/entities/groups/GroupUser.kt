package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("group_user")
class GroupUser(
    val groupId: UUID,
    val userId: UUID,
    val role: UserGroupRole,
) : SimpleEntity() {
    @Transient
    var user: UserEntity? = null
}
