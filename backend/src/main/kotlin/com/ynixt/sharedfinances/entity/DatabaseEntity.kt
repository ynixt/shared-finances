package com.ynixt.sharedfinances.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.*

@MappedSuperclass
abstract class DatabaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
) : AuditedEntity() {
    override fun hashCode(): Int {
        return if (id == null) super.hashCode() else Objects.hashCode(id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null) {
            return false
        }

        if (other is DatabaseEntity) {
            if (other::class == this::class && other.id == id) {
                return true
            }
        }

        return false
    }
}

