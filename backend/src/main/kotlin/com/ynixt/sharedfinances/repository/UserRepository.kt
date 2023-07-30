package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.User
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, Long> {
    @Query(
        """
        from User u
        left join fetch u.bankAccounts ub
        where u.id = :id
    """
    )
    fun findCurrentUserOneById(id: Long): User?
    fun findByUid(uid: String): User?
    fun findByEmail(uid: String): User?

    @Modifying
    fun save(user: User): User
}
