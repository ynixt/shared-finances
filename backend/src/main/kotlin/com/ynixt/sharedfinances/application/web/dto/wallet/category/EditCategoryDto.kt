package com.ynixt.sharedfinances.application.web.dto.wallet.category

import java.util.UUID

data class EditCategoryDto(
    val name: String,
    val color: String,
    val parentId: UUID?,
    val conceptId: UUID? = null,
    val customConceptName: String? = null,
)
