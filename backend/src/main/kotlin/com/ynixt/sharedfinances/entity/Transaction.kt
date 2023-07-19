package com.ynixt.sharedfinances.entity

import com.ynixt.sharedfinances.enums.TransactionType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    val type: TransactionType,

    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: TransactionCategory?,

    @ManyToOne
    @JoinColumn(name = "group_id")
    val group: Group?,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne
    @JoinColumn(name = "bank_account_id")
    val bankAccount: BankAccount?,

    @ManyToOne
    @JoinColumn(name = "credit_card_id")
    val creditCard: CreditCard?,
    val creditCardBillDate: String?,

    val date: ZonedDateTime,

    val value: BigDecimal,
    val description: String?,
    val installment: Int?,
    val totalInstallments: Int?
) : AuditedEntity()