package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.WalletItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface GroupWalletItemService {
    suspend fun findAllItems(
        userId: UUID,
        groupId: UUID,
        pageable: Pageable,
        onlyBankAccounts: Boolean = false,
    ): Page<WalletItem>
}
