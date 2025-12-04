package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import org.springframework.stereotype.Service

@Service
class GenericCategoryServiceImpl(
    override val repository: WalletEntryCategoryRepository,
) : EntityServiceImpl<WalletEntryCategoryEntity, WalletEntryCategoryEntity>(),
    GenericCategoryService
