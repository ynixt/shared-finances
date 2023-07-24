package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.BankAccount
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.CrudRepository

interface BankAccountRepository : CrudRepository<BankAccount, Long> {
    @Modifying
    fun deleteByIdAndUserId(id: Long, userId: Long)
}
