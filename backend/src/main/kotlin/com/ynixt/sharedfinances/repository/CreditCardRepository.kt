package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardLimitDto
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal

interface CreditCardRepository : CrudRepository<CreditCard, Long> {
    @Query(
        """
        from CreditCard c
        left join fetch c.billDates bd
        where
            c.id = :id
            and c.userId = :userId
    """
    )
    fun findOneByIdAndUserIdIncludeBillDates(id: Long, userId: Long): CreditCard?

    @Query(
        """
        from CreditCard c
        left join fetch c.billDates bd
        where c.userId = :userId
    """
    )
    fun findAllByUserIdIncludeBillDates(userId: Long): List<CreditCard>

    @Modifying
    fun deleteByIdAndUserId(id: Long, userId: Long)

    @Modifying
    @Query(
        """
        update CreditCard
        set availableLimit = availableLimit + :delta, updatedAt = offset_datetime
        where id = :id
        and userId = :userId
    """
    )
    fun addToAvailableLimit(id: Long, userId: Long, delta: BigDecimal)

    @Query(
        """
        select new com.ynixt.sharedfinances.model.dto.creditcard.CreditCardLimitDto(
            c.id,
            c.limit,
            c.availableLimit
        )
        from CreditCard c
        where c.id = :id
        and c.userId = :userId
    """
    )
    fun getCurrentAvailableLimit(id: Long, userId: Long): CreditCardLimitDto?
}
