package com.ynixt.sharedfinances.model.dto.transactioncategory

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserTransactionCategoryDto::class, name = "user_transaction_category"),
    JsonSubTypes.Type(value = GroupTransactionCategoryDto::class, name = "group_transaction_category")
)
interface TransactionCategoryDto

data class UserTransactionCategoryDto(
    val id: Long? = null,
    val name: String,
    val color: String,
    val userId: Long
) : TransactionCategoryDto

data class GroupTransactionCategoryDto(
    val id: Long? = null,
    val name: String,
    val color: String,
    val userId: Long,
    val groupId: Long,
) : TransactionCategoryDto
