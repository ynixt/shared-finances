package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class CreditCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
    var name: String,
    var closingDay: Int,
    var paymentDay: Int,
    var limit: BigDecimal,
    var availableLimit: BigDecimal?,
    var enabled: Boolean,
    var displayOnGroup: Boolean,
) : AuditedEntity() {
    @OneToMany(mappedBy = "creditCard")
    var billDates: MutableList<CreditCardBillDate> = mutableListOf()

    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null
}
