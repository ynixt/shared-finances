package com.ynixt.sharedfinances.entity

import com.ynixt.sharedfinances.enums.TransactionType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,

    @Enumerated(EnumType.STRING) val type: TransactionType,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id")
    var category: TransactionCategory? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id")
    var group: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    var user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_account_id")
    var bankAccount: BankAccount? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_card_id")
    var creditCard: CreditCard? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "credit_card_bill_date_id")
    var creditCardBillDate: CreditCardBillDate? = null,

    val date: ZonedDateTime,

    val value: BigDecimal, val description: String?, val installment: Int?, val totalInstallments: Int?
) : AuditedEntity() {
    @Column(name = "category_id", updatable = false, insertable = false)
    var categoryId: Long? = null

    @Column(name = "group_id", updatable = false, insertable = false)
    var groupId: Long? = null

    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null

    @Column(name = "bank_account_id", updatable = false, insertable = false)
    var bankAccountId: Long? = null

    @Column(name = "credit_card_id", updatable = false, insertable = false)
    var creditCardId: Long? = null

    @Column(name = "credit_card_bill_date_id", updatable = false, insertable = false)
    var creditCardBillDateId: Long? = null
}
