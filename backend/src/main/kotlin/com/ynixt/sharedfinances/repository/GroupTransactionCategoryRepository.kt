package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.GroupTransactionCategory
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository


interface GroupTransactionCategoryRepository : CrudRepository<GroupTransactionCategory, Long> {
    @Query(
        """
        from GroupTransactionCategory gct
        join gct.group g
        join g.users u
        where gct.id = :id and u.id = :userId
    """
    )
    fun findOneByUserIdAndGroupId(id: Long, userId: Long): GroupTransactionCategory?

    @Query(
        """
        from GroupTransactionCategory gct
        join gct.group g
        join g.users u
        where g.id = :groupId and u.id = :userId
    """
    )
    fun findAllByUserIdAndGroupId(userId: Long, groupId: Long): List<GroupTransactionCategory>
}
