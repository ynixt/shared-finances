package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupBankAccount
import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.BankAccountAlreadyInGroupException
import com.ynixt.sharedfinances.domain.repositories.BankAccountRepository
import com.ynixt.sharedfinances.domain.repositories.GroupBankAccountRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupWalletAssociationService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupWalletAssociationServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val bankAccountRepository: BankAccountRepository,
    private val groupBankAccountRepository: GroupBankAccountRepository,
    private val databaseHelperService: DatabaseHelperService,
) : GroupWalletAssociationService {
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
                    bankAccountRepository.findAllAllowedForGroup(groupId).collectList()
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
                    bankAccountRepository.findAllAssociatedToGroup(groupId).collectList()
                } else {
                    Mono.empty()
                }
            }

    override fun associate(
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
                    groupBankAccountRepository
                        .save(
                            GroupBankAccount(
                                groupId = groupId,
                                bankAccountId = bankAccountId,
                            ),
                        ).map { }
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
                    groupBankAccountRepository
                        .deleteByGroupIdAndBankAccountId(
                            groupId = groupId,
                            bankAccountId = bankAccountId,
                        ).map { }
                } else {
                    Mono.empty()
                }
            }
}
