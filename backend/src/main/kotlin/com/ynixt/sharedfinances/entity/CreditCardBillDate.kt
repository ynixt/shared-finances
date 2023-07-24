package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import java.sql.Date

@Entity
class CreditCardBillDate(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    val creditCard: CreditCard? = null,
    val billDate: Date
) : AuditedEntity() {
    @Column(name = "credit_card_id", updatable = false, insertable = false)
    var creditCardId: Long? = null
}