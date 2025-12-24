package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.RefreshTokenEntity
import com.ynixt.sharedfinances.domain.repositories.RefreshTokenRepository
import org.springframework.data.repository.Repository

interface RefreshTokenSpringDataRepository :
    RefreshTokenRepository,
    Repository<RefreshTokenEntity, String>
