package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptKind
import org.springframework.data.relational.core.mapping.Table

@Table("wallet_category_concept")
class WalletCategoryConceptEntity(
    val kind: WalletCategoryConceptKind,
    val code: WalletCategoryConceptCode?,
    val displayName: String?,
) : AuditedEntity()
