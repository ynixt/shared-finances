package com.ynixt.sharedfinances.application.web.dto.wallet.category

import java.util.UUID

data class NewCategoryDto(
    val name: String,
    val color: String,
    val parentId: UUID?,
)
