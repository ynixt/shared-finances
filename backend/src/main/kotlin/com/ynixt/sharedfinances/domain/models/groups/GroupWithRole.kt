package com.ynixt.sharedfinances.domain.models.groups

import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.time.OffsetDateTime
import java.util.UUID

data class GroupWithRole(
    var id: UUID?,
    var createdAt: OffsetDateTime?,
    var updatedAt: OffsetDateTime?,
    val name: String,
    val role: UserGroupRole,
    val itemsAssociated: List<WalletItem>? = null,
) {
    lateinit var permissions: Set<GroupPermissions>

    val itemsAssociatedIds: Set<UUID> = itemsAssociated?.map { it.id!! }?.toSet() ?: emptySet()
}
