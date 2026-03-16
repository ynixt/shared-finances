package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.R2dbcRepository

interface WalletEntrySpringDataRepository :
    EntityRepository<WalletEntryEntity>,
    R2dbcRepository<WalletEntryEntity, String>
