package com.ynixt.sharedfinances.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.*

@Entity
class GroupInvite(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    val group: Group?,

    val code: UUID,

    val expiresOn: OffsetDateTime
) : AuditedEntity() {
    @Column(name = "group_id", updatable = false, insertable = false)
    var groupId: Long? = null
}
