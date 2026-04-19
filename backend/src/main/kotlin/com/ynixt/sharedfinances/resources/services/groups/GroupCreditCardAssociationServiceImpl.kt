package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.entities.groups.GroupWalletItemEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.BankAccountAlreadyInGroupException
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupCreditCardAssociationService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupCreditCardAssociationServiceImpl(
    private val groupPermissionService: GroupPermissionService,
    private val walletItemRepository: WalletItemRepository,
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val databaseHelperService: DatabaseHelperService,
    private val groupActionEventService: GroupActionEventService,
    private val creditCardMapper: CreditCardMapper,
) : GroupCreditCardAssociationService {
    override suspend fun findAllAllowedCreditCardsToAssociate(
        userId: UUID,
        groupId: UUID,
    ): List<CreditCard> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_CREDIT_CARD,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAllowedForGroup(
                            userId = userId,
                            groupId = groupId,
                            type = WalletItemType.CREDIT_CARD,
                        ).collectList()
                        .awaitSingle()
                        .map(creditCardMapper::toModel)
                } else {
                    emptyList()
                }
            }

    override suspend fun findAllAssociatedCreditCards(
        userId: UUID,
        groupId: UUID,
    ): List<CreditCard> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .findAllAssociatedToGroup(
                            groupId,
                            WalletItemType.CREDIT_CARD,
                        ).collectList()
                        .awaitSingle()
                        .map(creditCardMapper::toModel)
                } else {
                    emptyList()
                }
            }

    override suspend fun associateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.ADD_CREDIT_CARD,
            ).let { hasPermission ->
                if (hasPermission) {
                    val walletItem = walletItemRepository.findOneById(creditCardId).awaitSingleOrNull() ?: return false
                    if (walletItem.type != WalletItemType.CREDIT_CARD) return false
                    if (walletItem.userId != userId) throw UnauthorizedException()

                    try {
                        groupWalletItemRepository
                            .save(
                                GroupWalletItemEntity(
                                    groupId = groupId,
                                    walletItemId = creditCardId,
                                ),
                            ).awaitSingle()
                            .also {
                                groupActionEventService
                                    .sendCreditCardAssociated(
                                        groupCreditCard = it,
                                        userId = userId,
                                    )
                            }
                    } catch (t: Throwable) {
                        throw if (databaseHelperService.isUniqueViolation(t, "idx_group_bank_account_group_id_bank_account")) {
                            BankAccountAlreadyInGroupException(
                                groupId = groupId,
                                bankAccountId = creditCardId,
                                cause = t,
                            )
                        } else {
                            t
                        }
                    }
                }

                hasPermission
            }

    override suspend fun unassociateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Boolean =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
                GroupPermissions.REMOVE_CREDIT_CARD,
            ).let { hasPermission ->
                if (hasPermission) {
                    groupWalletItemRepository
                        .deleteByGroupIdAndWalletItemId(
                            groupId = groupId,
                            walletItemId = creditCardId,
                        ).awaitSingle()
                        .also {
                            groupActionEventService
                                .sendCreditCardUnassociated(
                                    groupId = groupId,
                                    creditCardId = creditCardId,
                                    userId = userId,
                                )
                        }
                }

                hasPermission
            }
}
