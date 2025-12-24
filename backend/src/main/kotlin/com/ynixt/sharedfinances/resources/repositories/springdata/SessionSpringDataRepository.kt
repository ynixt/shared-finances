package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.SessionEntity
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import org.springframework.data.repository.Repository

interface SessionSpringDataRepository :
    SessionRepository,
    Repository<SessionEntity, String>
