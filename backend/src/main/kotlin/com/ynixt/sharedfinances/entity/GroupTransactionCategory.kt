package com.ynixt.sharedfinances.entity

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity()
open class GroupTransactionCategory(
    id: Long? = null,
    name: String,
    color: String,
    @ManyToOne
    @JoinColumn(name = "group_id")
    val group: Group,
) : TransactionCategory(id = id, name = name, color = color)