package com.ynixt.sharedfinances.entity

import jakarta.persistence.*

@Entity
class Group(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var name: String,
) : AuditedEntity() {
    @ManyToMany
    @JoinTable(
        name = "group_has_users",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var users: MutableList<User>? = null
}
