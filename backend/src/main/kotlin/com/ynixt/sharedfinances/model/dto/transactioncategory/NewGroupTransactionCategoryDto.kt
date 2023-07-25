package com.ynixt.sharedfinances.model.dto.transactioncategory

data class NewGroupTransactionCategoryDto(
    val name: String,
    val color: String,
    val groupId: Long
)
