package com.ynixt.sharedfinances.domain.models.groups

import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest

data class NewGroupRequest(
    val name: String,
    val categories: List<NewCategoryRequest>?,
)
