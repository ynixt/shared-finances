package com.ynixt.sharedfinances.service

import com.ynixt.sharedfinances.entity.GroupInvite
import com.ynixt.sharedfinances.entity.User

interface GroupInviteService {
    fun generateInvite(
        user: User,
        groupId: Long,
    ): GroupInvite

    fun useInvite(
        user: User,
        code: String,
    ): Long?

    fun deleteAllExpiredInvites()
}
