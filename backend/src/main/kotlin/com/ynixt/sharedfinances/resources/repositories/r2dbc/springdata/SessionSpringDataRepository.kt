package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import org.springframework.data.r2dbc.repository.R2dbcRepository

interface SessionSpringDataRepository :
    SessionRepository,
    R2dbcRepository<SessionEntity, String>
