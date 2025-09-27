package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupBankAccount
import com.ynixt.sharedfinances.domain.repositories.GroupBankAccountRepository
import org.springframework.data.repository.Repository

interface GroupBankAccountSpringDataRepository :
    GroupBankAccountRepository,
    Repository<GroupBankAccount, String>
