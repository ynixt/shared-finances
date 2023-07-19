package com.ynixt.sharedfinances.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class UserTransactionCategory(
    id: Long? = null,
    name: String,
    color: String,
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,
) : TransactionCategory(id = id, name = name, color = color)