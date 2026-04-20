package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryConceptDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.EditCategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto
import com.ynixt.sharedfinances.application.web.mapper.CategoryDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CategoryDtoMapperImpl : CategoryDtoMapper {
    override fun toDto(from: WalletEntryCategoryEntity): CategoryDto = CategoryToDtoMapper.map(from)

    override fun toConceptDto(from: WalletCategoryConceptEntity): CategoryConceptDto = CategoryConceptToDtoMapper.map(from)

    override fun fromNewDtoToNewRequest(from: NewCategoryDto): NewCategoryRequest = CategoryFromNewDtoMapper.map(from)

    override fun fromEditDtoToEditRequest(from: EditCategoryDto): EditCategoryRequest = CategoryFromEditDtoMapper.map(from)

    private object CategoryToDtoMapper : ObjectMappie<WalletEntryCategoryEntity, CategoryDto>() {
        override fun map(from: WalletEntryCategoryEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object CategoryConceptToDtoMapper : ObjectMappie<WalletCategoryConceptEntity, CategoryConceptDto>() {
        override fun map(from: WalletCategoryConceptEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object CategoryFromNewDtoMapper : ObjectMappie<NewCategoryDto, NewCategoryRequest>() {
        override fun map(from: NewCategoryDto) =
            mapping {
            }
    }

    private object CategoryFromEditDtoMapper : ObjectMappie<EditCategoryDto, EditCategoryRequest>() {
        override fun map(from: EditCategoryDto) = mapping {}
    }
}
