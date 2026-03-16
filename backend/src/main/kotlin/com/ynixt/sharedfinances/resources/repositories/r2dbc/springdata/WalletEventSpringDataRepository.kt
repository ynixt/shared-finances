package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import org.springframework.data.r2dbc.repository.R2dbcRepository

interface WalletEventSpringDataRepository : R2dbcRepository<WalletEventEntity, String>
