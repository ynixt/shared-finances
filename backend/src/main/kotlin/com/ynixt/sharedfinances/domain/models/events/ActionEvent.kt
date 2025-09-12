package com.ynixt.sharedfinances.domain.models.events

import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import java.util.UUID

abstract class ActionEvent<T> {
    abstract val id: UUID
    abstract val type: ActionEventType
    abstract val category: ActionEventCategory
    abstract val data: T
    abstract val userId: UUID
}
