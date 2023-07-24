package com.ynixt.sharedfinances.entity

import jakarta.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "type", discriminatorType = DiscriminatorType.STRING
)
abstract class TransactionCategory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val name: String,
    val color: String,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") var user: User? = null,

    ) : AuditedEntity() {
    @Column(name = "user_id", updatable = false, insertable = false)
    var userId: Long? = null
}
