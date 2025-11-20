package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import org.springframework.data.repository.Repository

interface CreditCardBillSpringDataRepository :
    CreditCardBillRepository,
    Repository<CreditCardBillEntity, String>
