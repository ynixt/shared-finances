package com.ynixt.sharedfinances.repository

import com.ynixt.sharedfinances.entity.GroupInvite
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.OffsetDateTime
import java.util.*

interface GroupInviteRepository : CrudRepository<GroupInvite, Long> {
    @Query(
        """
        from GroupInvite gi
        join fetch gi.group g
        where
            gi.code = :code
            and gi.expiresOn > :expiresOn
    """
    )
    fun findOneByCodeAndExpiresOnGreaterThanWithGroup(
        code: UUID,
        expiresOn: OffsetDateTime
    ): GroupInvite?

    fun deleteOneById(id: Long): Long

    @Modifying
    fun deleteByExpiresOnLessThanEqual(expiresOn: OffsetDateTime): Long
}
