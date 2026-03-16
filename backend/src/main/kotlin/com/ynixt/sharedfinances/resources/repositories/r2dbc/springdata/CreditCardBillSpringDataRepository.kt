package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface CreditCardBillSpringDataRepository :
    CreditCardBillRepository,
    R2dbcRepository<CreditCardBillEntity, String> {
    @Query(
        """
            select bill.*
            from credit_card_bill bill
            join wallet_item creditCard on creditCard.id = bill.credit_card_id
            where 
                bill.credit_card_id = :creditCardId
                and bill.bill_date = :billDate
                and creditCard.user_id = :userId
        """,
    )
    override fun findOneByUserIdAndCreditCardIdAndBillDate(
        userId: UUID,
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity>

    @Modifying
    @Query(
        """
        update credit_card_bill 
        set 
            value = value + :value,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun addValueById(
        id: UUID,
        value: BigDecimal,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        update credit_card_bill 
        set 
            closing_date = :closingDate,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun changeClosingDateById(
        id: UUID,
        closingDate: LocalDate,
    ): Mono<Long>

    @Modifying
    @Query(
        """
        update credit_card_bill 
        set 
            due_date = :dueDate,
            updated_at = CURRENT_TIMESTAMP
        where id = :id
    """,
    )
    override fun changeDueDateById(
        id: UUID,
        dueDate: LocalDate,
    ): Mono<Long>
}
