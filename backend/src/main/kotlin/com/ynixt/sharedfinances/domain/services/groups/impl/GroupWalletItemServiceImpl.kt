package com.ynixt.sharedfinances.domain.services.groups.impl

import com.ynixt.sharedfinances.domain.models.WalletItemSearchResponse
import com.ynixt.sharedfinances.domain.repositories.GroupWalletItemRepository
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupWalletItemService
import com.ynixt.sharedfinances.domain.util.PageUtil.createPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupWalletItemServiceImpl(
    private val groupWalletItemRepository: GroupWalletItemRepository,
    private val groupPermissionService: GroupPermissionService,
) : GroupWalletItemService {
    override fun findAllItems(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
    ): Mono<Page<WalletItemSearchResponse>> =
        groupPermissionService
            .hasPermission(
                userId = userId,
                groupId = groupId,
            ).flatMap { hasPermission ->
                if (hasPermission) {
                    createPage(pageable, countFn = { groupWalletItemRepository.countByGroupId(groupId, enabled = true) }) {
                        groupWalletItemRepository.findAllByGroupIdAndEnabled(
                            groupId = groupId,
                            enabled = true,
                            pageable = pageable,
                        )
                    }
                } else {
                    Mono.empty()
                }
            }
}
