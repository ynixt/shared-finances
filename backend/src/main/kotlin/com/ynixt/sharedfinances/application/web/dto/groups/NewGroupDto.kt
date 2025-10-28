package com.ynixt.sharedfinances.application.web.dto.groups

import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto

data class NewGroupDto(
    val name: String,
    val categories: List<NewCategoryDto>?,
)
