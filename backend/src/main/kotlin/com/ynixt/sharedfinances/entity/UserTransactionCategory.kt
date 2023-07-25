package com.ynixt.sharedfinances.entity

import jakarta.persistence.*

@Entity
@DiscriminatorValue("UserTransactionCategory")
class UserTransactionCategory(
    id: Long? = null,
    name: String,
    color: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,
) : TransactionCategory(id = id, name = name, color = color) {
    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null
}
