package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupCreditCard
import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.exceptions.BankAccountAlreadyInGroupException
import com.ynixt.sharedfinances.domain.repositories.CreditCardRepository
import com.ynixt.sharedfinances.domain.repositories.GroupCreditCardRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupCreditCardAssociationServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val creditCardRepository: CreditCardRepository,
    private val groupCreditCardRepository: GroupCreditCardRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val groupActionEventService: GroupActionEventService,
) : GroupCreditCardAssociationService {
    override fun findAllAllowedCreditCardsToAssociate(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<CreditCard>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_CREDIT_CARD,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    creditCardRepository.findAllAllowedForGroup(groupId).collectList()
                } else {
                    Mono.empty()
                }
            }

    override fun findAllAssociatedCreditCards(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<CreditCard>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    creditCardRepository.findAllAssociatedToGroup(groupId).collectList()
                } else {
                    Mono.empty()
                }
            }

    override fun associateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Unit> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_CREDIT_CARD,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupCreditCardRepository
                        .save(
                            GroupCreditCard(
                                groupId = groupId,
                                creditCardId = creditCardId,
                            ),
                        ).flatMap {
                            groupActionEventService
                                .sendCreditCardAssociated(
                                    groupCreditCard = it,
                                    userId = userId,
                                )
                        }.map { }
                        .onErrorMap { t ->
                            if (databaseHelperService.isUniqueViolation(t, "idx_group_bank_account_group_id_bank_account")) {
                                BankAccountAlreadyInGroupException(
                                    groupId = groupId,
                                    bankAccountId = creditCardId,
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

    override fun unassociateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Unit> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.REMOVE_CREDIT_CARD,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    groupCreditCardRepository
                        .deleteByGroupIdAndCreditCardId(
                            groupId = groupId,
                            creditCardId = creditCardId,
                        ).flatMap {
                            groupActionEventService
                                .sendCreditCardUnassociated(
                                    groupId = groupId,
                                    creditCardId = creditCardId,
                                    userId = userId,
                                )
                        }.map { }
                } else {
                    Mono.empty()
                }
            }
}
