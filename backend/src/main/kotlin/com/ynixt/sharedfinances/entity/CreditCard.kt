package com.ynixt.sharedfinances.entity

import com.ynixt.sharedfinances.config.hibernatetypes.StringArrayType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Type
import java.math.BigDecimal

@Entity
class CreditCard(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    val name: String,
    val closingDay: Int,
    val paymentDay: Int,
    val limit: BigDecimal,
    @Type(StringArrayType::class)
    val billDates: List<String>?,
    val availableLimit: BigDecimal?,
    val enabled: Boolean,
    val displayOnGroup: Boolean,
) : AuditedEntity()