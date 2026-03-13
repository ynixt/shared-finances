package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.repository.Repository

interface WalletEntrySpringDataRepository :
    EntityRepository<WalletEntryEntity>,
    Repository<WalletEntryEntity, String>
