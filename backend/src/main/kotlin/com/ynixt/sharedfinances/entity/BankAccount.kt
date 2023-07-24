package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class BankAccount(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
    var name: String,
    val balance: BigDecimal = BigDecimal.ZERO,
    val enabled: Boolean,
    val displayOnGroup: Boolean
) : AuditedEntity() {
    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null
}
