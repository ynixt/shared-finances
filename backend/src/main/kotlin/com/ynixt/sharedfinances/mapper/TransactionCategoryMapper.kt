package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import com.ynixt.sharedfinances.entity.TransactionCategory
import com.ynixt.sharedfinances.entity.UserTransactionCategory
import com.ynixt.sharedfinances.model.dto.transactioncategory.GroupTransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.TransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.UserTransactionCategoryDto
import org.mapstruct.Mapper
import org.mapstruct.Named
import org.mapstruct.ObjectFactory

@Mapper
interface TransactionCategoryMapper {
    @ObjectFactory
    fun createTransactionCategoryDto(entity: TransactionCategory): TransactionCategoryDto {
        return when (entity) {
            is UserTransactionCategory -> toDtoUser(entity)
            is GroupTransactionCategory -> toDtoGroup(entity)
            else -> TODO()
        }
    }


    fun toDto(entity: TransactionCategory): TransactionCategoryDto
    fun toDtoList(entity: List<TransactionCategory>): List<TransactionCategoryDto>

    @Named("toDtoUserTransactionCategory")
    fun toDtoUser(entity: UserTransactionCategory): UserTransactionCategoryDto

    @Named("toDtoGroupTransactionCategory")
    fun toDtoGroup(entity: GroupTransactionCategory): GroupTransactionCategoryDto
}
