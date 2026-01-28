package com.ynixt.sharedfinances.application.web.dto.user

import com.ynixt.sharedfinances.application.web.dto.wallet.category.NewCategoryDto

data class UserOnboardingDto(
    val categories: List<NewCategoryDto>,
)
