package com.ynixt.sharedfinances.application.web.dto.wallet.category

import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptKind
import java.util.UUID

data class CategoryConceptDto(
    val id: UUID,
    val kind: WalletCategoryConceptKind,
    val code: WalletCategoryConceptCode?,
    val displayName: String?,
)
