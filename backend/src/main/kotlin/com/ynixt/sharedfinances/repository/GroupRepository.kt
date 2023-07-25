package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.Group
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface GroupRepository : CrudRepository<Group, Long> {
    @Query(
        """
        from Group g
        join g.users u
        where u.id = :userId
    """
    )
    fun getAllByUserId(userId: Long): List<Group>

    @Query(
        """
        from Group g
        join g.users u
        where g.id = :id and u.id = :userId
    """
    )
    fun getOneByIdAndUserId(id: Long, userId: Long): Group?

    @Query(
        """
        select case when count(1) > 0 then true else false end
        from Group g
        join g.users u
        where g.id = :id and u.id = :userId
    """
    )
    fun existsOneByIdAndUserId(id: Long, userId: Long): Boolean

    @Query(
        """
        from Group g
        join fetch g.users u
        where g.id = :id and u.id = :userId
    """
    )
    fun getOneByIdAndUserIdWithUsers(id: Long, userId: Long): Group?
}
