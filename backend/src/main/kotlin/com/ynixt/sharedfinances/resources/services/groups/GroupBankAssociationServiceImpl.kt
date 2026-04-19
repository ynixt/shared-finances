package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.BankAccountAlreadyInGroupException
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupBankAssociationServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val walletItemRepository: WalletItemRepository,
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val groupActionEventService: GroupActionEventService,
    private val bankAccountMapper: BankAccountMapper,
) : GroupBankAssociationService {
    override suspend fun findAllAllowedBanksToAssociate(
        userId: UUID,
        groupId: UUID,
    ): List<BankAccount> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_BANK_ACCOUNT,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAllowedForGroup(
                            userId = userId,
                            groupId = groupId,
                            type = WalletItemType.BANK_ACCOUNT,
                        ).collectList()
                        .awaitSingle()
                        .map(bankAccountMapper::toModel)
                } else {
                    emptyList()
                }
            }

    override suspend fun findAllAssociatedBanks(
        userId: UUID,
        groupId: UUID,
    ): List<BankAccount> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAssociatedToGroup(
                            groupId,
                            WalletItemType.BANK_ACCOUNT,
                        ).collectList()
                        .awaitSingle()
                        .map(bankAccountMapper::toModel)
                } else {
                    emptyList()
                }
            }

    override suspend fun associateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_BANK_ACCOUNT,
            ).let { hasPermission ->
                if (hasPermission) {
                    val walletItem = walletItemRepository.findOneById(bankAccountId).awaitSingleOrNull() ?: return false
                    if (walletItem.type != WalletItemType.BANK_ACCOUNT) return false
                    if (walletItem.userId != userId) throw UnauthorizedException()

                    try {
                        groupWalletItemRepository
                            .save(
                                GroupWalletItemEntity(
                                    groupId = groupId,
                                    walletItemId = bankAccountId,
                                ),
                            ).awaitSingle()
                            .also {
                                groupActionEventService
                                    .sendBankAssociated(
                                        groupBankAccount = it,
                                        userId = userId,
                                    )
                            }
                    } catch (t: Throwable) {
                        throw if (databaseHelperService.isUniqueViolation(t, "idx_group_bank_account_group_id_bank_account")) {
                            BankAccountAlreadyInGroupException(
                                groupId = groupId,
                                bankAccountId = bankAccountId,
                                cause = t,
                            )
                        } else {
                            t
                        }
                    }
                }

                return hasPermission
            }

    override suspend fun unassociateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.REMOVE_BANK_ACCOUNT,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .deleteByGroupIdAndWalletItemId(
                            groupId = groupId,
                            walletItemId = bankAccountId,
                        ).also {
                            groupActionEventService
                                .sendBankUnassociated(
                                    groupId = groupId,
                                    bankAccountId = bankAccountId,
                                    userId = userId,
                                )
                        }
                }

                return hasPermission
            }
}
