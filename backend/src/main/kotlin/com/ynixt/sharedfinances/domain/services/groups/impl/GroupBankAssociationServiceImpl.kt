package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItem
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.BankAccountAlreadyInGroupException
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupBankAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
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
    override fun findAllAllowedBanksToAssociate(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<BankAccount>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_BANK_ACCOUNT,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAllowedForGroup(
                            groupId,
                            WalletItemType.BANK_ACCOUNT,
                        ).map(bankAccountMapper::toModel)
                        .collectList()
                } else {
                    Mono.empty()
                }
            }

    override fun findAllAssociatedBanks(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<BankAccount>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAssociatedToGroup(
                            groupId,
                            WalletItemType.BANK_ACCOUNT,
                        ).map(bankAccountMapper::toModel)
                        .collectList()
                } else {
                    Mono.empty()
                }
            }

    override fun associateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Unit> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_BANK_ACCOUNT,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .save(
                            GroupWalletItem(
                                groupId = groupId,
                                walletItemId = bankAccountId,
                            ),
                        ).flatMap {
                            groupActionEventService
                                .sendBankAssociated(
                                    groupBankAccount = it,
                                    userId = userId,
                                )
                        }.map { }
                        .onErrorMap { t ->
                            if (databaseHelperService.isUniqueViolation(t, "idx_group_bank_account_group_id_bank_account")) {
                                BankAccountAlreadyInGroupException(
                                    groupId = groupId,
                                    bankAccountId = bankAccountId,
                                    cause = t,
                                )
                            } else {
                                t
                            }
                        }
                } else {
                    Mono.empty()
                }
            }

    override fun unassociateBank(
        userId: UUID,
        groupId: UUID,
        bankAccountId: UUID,
    ): Mono<Unit> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.REMOVE_BANK_ACCOUNT,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .deleteByGroupIdAndWalletItemId(
                            groupId = groupId,
                            walletItemId = bankAccountId,
                        ).flatMap {
                            groupActionEventService
                                .sendBankUnassociated(
                                    groupId = groupId,
                                    bankAccountId = bankAccountId,
                                    userId = userId,
                                )
                        }.map { }
                } else {
                    Mono.empty()
                }
            }
}
