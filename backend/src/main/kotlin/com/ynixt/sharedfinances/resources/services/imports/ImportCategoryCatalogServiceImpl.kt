package com.ynixt.sharedfinances.resources.services.imports

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.imports.ImportCategoryCatalogService
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImportCategoryCatalogServiceImpl(
    private val categoryRepository: WalletEntryCategoryRepository,
    private val groupUsersRepository: GroupUsersRepository,
) : ImportCategoryCatalogService {
    override suspend fun findAll(userId: UUID): List<WalletEntryCategoryEntity> =
        categoryRepository.findAllAvailableForImport(userId).collectList().awaitSingle()

    override suspend fun findAllMembers(userId: UUID): List<GroupUserEntity> =
        groupUsersRepository.findAllMembersForUser(userId).collectList().awaitSingle()
}
