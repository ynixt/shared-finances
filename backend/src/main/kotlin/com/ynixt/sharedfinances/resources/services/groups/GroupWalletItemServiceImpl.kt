package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupWalletItemService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupWalletItemServiceImpl(
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val groupPermissionService: GroupPermissionService,
    private val walletItemMapper: WalletItemMapper,
) : GroupWalletItemService {
    override suspend fun findAllItems(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Page<WalletItem> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).let { hasPermission ->
                if (hasPermission) {
                    createPage(pageable, countFn = { groupWalletItemRepository.countByGroupId(groupId, enabled = true) }) {
                        groupWalletItemRepository
                            .findAllByGroupIdAndEnabled(
                                groupId = groupId,
                                enabled = true,
                                pageable = pageable,
                            ).map(walletItemMapper::toModel)
                    }
                } else {
                    Page.empty()
                }
            }
}
