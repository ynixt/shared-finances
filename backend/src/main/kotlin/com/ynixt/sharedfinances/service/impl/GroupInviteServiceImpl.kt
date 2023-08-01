package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.Group
import com.ynixt.sharedfinances.entity.GroupInvite
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.exceptions.SFExceptionForbidden
import com.ynixt.sharedfinances.repository.GroupInviteRepository
import com.ynixt.sharedfinances.service.GroupInviteService
import com.ynixt.sharedfinances.service.GroupService
import jakarta.persistence.EntityManager
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class GroupInviteServiceImpl(
    private val groupService: GroupService,
    private val entityManager: EntityManager,
    private val groupInviteRepository: GroupInviteRepository,
    @Value("\${sf-app.expires-invite-after-min}") private val expiresInviteAfterMin: Long
) : GroupInviteService {
    private val logger = LogFactory.getLog(javaClass)

    @Transactional
    override fun generateInvite(user: User, groupId: Long): GroupInvite {
        if (!groupService.userHasPermissionToGroup(user, groupId)) {
            throw SFExceptionForbidden()
        }

        return groupInviteRepository.save(
            GroupInvite(
                group = entityManager.getReference(Group::class.java, groupId),
                code = UUID.randomUUID(),
                expiresOn = OffsetDateTime.now().plusMinutes(expiresInviteAfterMin)
            )
        )
    }

    @Transactional
    override fun useInvite(user: User, code: String): Long? {
        val invite = groupInviteRepository.findOneByCodeAndExpiresOnGreaterThanWithGroup(
            UUID.fromString(code),
            OffsetDateTime.now()
        )

        if (invite != null) {
            val group = invite.group!!

            if (groupService.userHasPermissionToGroup(groupId = group.id!!, user = user)) {
                return group.id
            }

            if (groupInviteRepository.deleteOneById(invite.id!!) > 0) {
                if (group.users != null) {
                    group.users!!.add(user)
                } else {
                    group.users = mutableListOf(user)
                }

                groupService.save(group)
                return group.id
            }
        }

        return null
    }

    @Transactional
    override fun deleteAllExpiredInvites() {
        val deleted = groupInviteRepository.deleteByExpiresOnLessThanEqual(OffsetDateTime.now())

        logger.info("$deleted group invites was deleted.")
    }
}
