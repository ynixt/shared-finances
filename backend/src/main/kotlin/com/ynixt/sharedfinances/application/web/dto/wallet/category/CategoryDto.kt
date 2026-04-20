package com.ynixt.sharedfinances.application.web.dto.wallet.category

import java.util.UUID

data class CategoryDto(
    val id: UUID,
    val name: String,
    val color: String,
    var children: List<CategoryDto>?,
    val parentId: UUID?,
    val conceptId: UUID,
)
