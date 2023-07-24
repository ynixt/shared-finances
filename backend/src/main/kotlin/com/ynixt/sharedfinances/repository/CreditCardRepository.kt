package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.CreditCard
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.CrudRepository

interface CreditCardRepository : CrudRepository<CreditCard, Long>, JpaSpecificationExecutor<CreditCard> {
    fun findOneByIdAndUserId(id: Long, userId: Long): CreditCard?
    fun findAllByUserId(userId: Long): List<CreditCard>

    @Modifying
    fun deleteByIdAndUserId(id: Long, userId: Long)
}
