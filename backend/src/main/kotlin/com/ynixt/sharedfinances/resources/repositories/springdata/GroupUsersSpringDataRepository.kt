package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.GroupUsers
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import org.springframework.data.repository.CrudRepository

interface GroupUsersSpringDataRepository :
    GroupUsersRepository,
    CrudRepository<GroupUsers, String>
