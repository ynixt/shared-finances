package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.repositories.GroupInviteRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface GroupInviteSpringDataRepository :
    GroupInviteRepository,
    ReactiveCrudRepository<GroupInvite, String>
