package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.category.CategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.EditCategoryDto
import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategory
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest

interface CategoryDtoMapper {
    fun toDto(from: WalletEntryCategory): CategoryDto

    fun fromNewDtoToNewRequest(from: NewCategoryDto): NewCategoryRequest

    fun fromEditDtoToEditRequest(from: EditCategoryDto): EditCategoryRequest
}
