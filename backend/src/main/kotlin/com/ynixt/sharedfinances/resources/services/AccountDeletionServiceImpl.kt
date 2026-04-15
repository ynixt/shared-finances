package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.simulation.SimulationJobService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AccountDeletionServiceImpl(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val groupUsersRepository: GroupUsersRepository,
    private val groupActionEventService: GroupActionEventService,
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val walletEventRepository: WalletEventRepository,
    private val recurrenceEventRepository: RecurrenceEventRepository,
    private val simulationJobService: SimulationJobService,
    private val sessionRepository: SessionRepository,
    private val avatarService: AvatarService,
) : AccountDeletionService {
    @Transactional
    override suspend fun deleteAccountForUser(userId: UUID) {
        userRepository.findById(userId).awaitSingleOrNull() ?: return

        sessionRepository.deleteAllByUserId(userId).awaitSingle()

        val memberships = groupRepository.findAllByUserIdOrderByName(userId).collectList().awaitSingle()

        // Unshare wallet items and purge ledger data so FKs (wallet_entry → wallet_item, group_wallet_item → wallet_item)
        // do not block CASCADE deletion of wallet_item when the user row is removed.
        groupWalletItemRepository.deleteAllForWalletItemsOwnedByUser(userId).awaitSingle()
        recurrenceEventRepository.deleteAllForAccountDeletion(userId).awaitSingle()
        walletEventRepository.deleteAllForAccountDeletion(userId).awaitSingle()

        simulationJobService.cancelAndRemoveAllJobsLinkedToUserForCompliance(userId)

        for (gw in memberships) {
            val groupId = gw.id ?: continue
            if (!groupRepository.existsById(groupId).awaitSingle()) {
                continue
            }

            val members = groupUsersRepository.findAllMembers(groupId).collectList().awaitSingle()
            val me = members.find { it.userId == userId } ?: continue
            val memberUserIds = members.map { it.userId }

            if (members.size == 1) {
                deleteGroupInternal(
                    groupId = groupId,
                    actingUserId = userId,
                    memberUserIds = memberUserIds,
                )
                continue
            }

            if (me.role == UserGroupRole.ADMIN) {
                val hasOtherAdmin = members.any { it.userId != userId && it.role == UserGroupRole.ADMIN }
                if (!hasOtherAdmin) {
                    val others = members.filter { it.userId != userId }.sortedWith(compareBy({ it.id }))
                    val promotee =
                        others.firstOrNull { it.role == UserGroupRole.EDITOR }
                            ?: others.firstOrNull { it.role == UserGroupRole.VIEWER }
                    if (promotee == null) {
                        deleteGroupInternal(
                            groupId = groupId,
                            actingUserId = userId,
                            memberUserIds = memberUserIds,
                        )
                        continue
                    }
                    groupUsersRepository.updateRole(promotee.userId, groupId, UserGroupRole.ADMIN).awaitSingle()
                }
            }

            groupUsersRepository.deleteByGroupIdAndUserId(groupId, userId).awaitSingle()
        }

        runCatching { avatarService.deletePhoto(userId) }

        userRepository.deleteById(userId).awaitSingle()
    }

    private suspend fun deleteGroupInternal(
        groupId: UUID,
        actingUserId: UUID,
        memberUserIds: List<UUID>,
    ) {
        val deleted = groupRepository.deleteById(groupId).awaitSingle()
        if (deleted > 0) {
            groupActionEventService.sendDeletedGroup(
                userId = actingUserId,
                id = groupId,
                membersId = memberUserIds,
            )
        }
    }
}
