package com.ynixt.sharedfinances.domain.models.category

import java.util.UUID

data class EditCategoryRequest(
    val name: String,
    val color: String,
    val parentId: UUID?,
)
