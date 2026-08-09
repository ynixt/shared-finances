package com.ynixt.sharedfinances.resources.services

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

        val ownedGroups = groupRepository.findAllByOwnerUserId(userId).collectList().awaitSingle()
        val ownedGroupIds = ownedGroups.mapNotNull { it.id }.toSet()

        // Owned groups must be deleted before the users row because group.owner_user_id uses ON DELETE RESTRICT.
        for (group in ownedGroups) {
            val groupId = group.id ?: continue
            val memberUserIds =
                groupUsersRepository
                    .findAllMembers(groupId)
                    .map { it.userId }
                    .collectList()
                    .awaitSingle()
            deleteGroupInternal(
                groupId = groupId,
                actingUserId = userId,
                memberUserIds = memberUserIds,
            )
        }

        for (membership in memberships) {
            val groupId = membership.id ?: continue
            if (groupId !in ownedGroupIds) {
                groupUsersRepository.deleteByGroupIdAndUserId(groupId, userId).awaitSingle()
            }
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
