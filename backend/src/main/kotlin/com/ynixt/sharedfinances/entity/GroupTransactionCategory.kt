package com.ynixt.sharedfinances.entity

import jakarta.persistence.*

@Entity
@DiscriminatorValue("GroupTransactionCategory")
class GroupTransactionCategory(
    id: Long? = null,
    name: String,
    color: String,
    user: User?,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id") var group: Group? = null
) : TransactionCategory(id = id, name = name, color = color, user = user) {
    @Column(name = "group_id", updatable = false, insertable = false)
    var groupId: Long? = null
}
