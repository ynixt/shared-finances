package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class BankAccount(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    val name: String,
    val balance: BigDecimal,
    val enabled: Boolean,
    val displayOnGroup: Boolean
): AuditedEntity()