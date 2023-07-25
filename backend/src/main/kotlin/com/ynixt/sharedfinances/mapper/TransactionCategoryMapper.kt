package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import com.ynixt.sharedfinances.entity.TransactionCategory
import com.ynixt.sharedfinances.entity.UserTransactionCategory
import com.ynixt.sharedfinances.model.dto.transactioncategory.*
import org.mapstruct.*

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface TransactionCategoryMapper {
    @ObjectFactory
    fun createTransactionCategoryDto(entity: TransactionCategory?): TransactionCategoryDto? {
        return if (entity == null) return null else when (entity) {
            is UserTransactionCategory -> toDtoUser(entity)
            is GroupTransactionCategory -> toDtoGroup(entity)
            else -> TODO()
        }
    }


    fun toDto(entity: TransactionCategory?): TransactionCategoryDto?
    fun toDtoList(entity: List<TransactionCategory>?): List<TransactionCategoryDto>?

    @Named("toDtoUserTransactionCategory")
    fun toDtoUser(entity: UserTransactionCategory?): UserTransactionCategoryDto?

    fun toDtoUserList(entity: List<UserTransactionCategory>?): List<UserTransactionCategoryDto>?

    fun updateUser(
        @MappingTarget transactionCategory: UserTransactionCategory?, updateDto: UpdateUserTransactionCategoryDto?
    )

    @Named("toDtoGroupTransactionCategory")
    fun toDtoGroup(entity: GroupTransactionCategory?): GroupTransactionCategoryDto?

    fun toDtoGroupList(entity: List<GroupTransactionCategory>?): List<GroupTransactionCategoryDto>?

    fun updateGroup(
        @MappingTarget transactionCategory: GroupTransactionCategory?, updateDto: UpdateGroupTransactionCategoryDto?
    )
}
