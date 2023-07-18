package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.User
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.Repository

interface UserRepository : Repository<User, Long> {
    fun findByUid(uid: String): User?
    fun findByEmail(uid: String): User?

    @Modifying
    fun save(user: User): User
}