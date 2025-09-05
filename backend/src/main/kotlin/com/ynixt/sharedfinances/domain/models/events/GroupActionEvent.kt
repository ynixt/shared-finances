package com.ynixt.sharedfinances.domain.models.events

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import java.util.UUID

data class GroupActionEvent<T>(
    override val id: UUID = UUID.randomUUID(),
    override val type: ActionEventType,
    override val category: ActionEventCategory,
    override val data: T,
    val modifiedByUserId: UUID,
    val groupId: UUID,
) : ActionEvent<T>()
