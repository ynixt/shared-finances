package com.ynixt.sharedfinances.entity

import jakarta.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "type", discriminatorType = DiscriminatorType.STRING
)
abstract class TransactionCategory(
    id: Long? = null,
    var name: String,
    var color: String,
) : DatabaseEntity(id)
