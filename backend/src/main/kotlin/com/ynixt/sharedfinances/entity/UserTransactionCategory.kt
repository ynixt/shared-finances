package com.ynixt.sharedfinances.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("UserTransactionCategory")
class UserTransactionCategory(
    id: Long? = null,
    name: String,
    color: String,
    user: User?
) : TransactionCategory(id = id, name = name, color = color, user = user)
