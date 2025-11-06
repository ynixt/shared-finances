package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.groups.GroupCreditCard
import com.ynixt.sharedfinances.domain.repositories.GroupCreditCardRepository
import org.springframework.data.repository.Repository

interface GroupCreditCardSpringDataRepository :
    GroupCreditCardRepository,
    Repository<GroupCreditCard, String>
