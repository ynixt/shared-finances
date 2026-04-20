package com.ynixt.sharedfinances.domain.models.category

import java.util.UUID

data class NewCategoryRequest(
    val name: String,
    val color: String,
    val parentId: UUID?,
    val conceptId: UUID? = null,
    val customConceptName: String? = null,
)
