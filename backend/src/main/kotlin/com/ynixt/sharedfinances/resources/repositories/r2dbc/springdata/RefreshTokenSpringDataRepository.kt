package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import com.ynixt.sharedfinances.domain.repositories.RefreshTokenRepository
import org.springframework.data.r2dbc.repository.R2dbcRepository

interface RefreshTokenSpringDataRepository :
    RefreshTokenRepository,
    R2dbcRepository<RefreshTokenEntity, String>
