package com.ynixt.sharedfinances.resources.services.categories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptKind
import com.ynixt.sharedfinances.domain.exceptions.http.DebtSfCategoryProtectedException
import com.ynixt.sharedfinances.domain.exceptions.http.DuplicatedCategoryConceptException
import com.ynixt.sharedfinances.domain.models.category.EditCategoryRequest
import com.ynixt.sharedfinances.domain.models.category.NewCategoryRequest
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCategoryRepository
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.UserCategoryActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

class CategoryServiceDebtSfProtectionTest {
    @Test
    fun `user deleteCategory should reject DEBT_SF category`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val category = category(id = categoryId, userId = userId, conceptId = debtConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                UserCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService = FakeCategoryConceptService(debtConceptId = debtConceptId),
                    databaseHelperService = NoOpDatabaseHelperService,
                    userCategoryActionEventService = NoOpUserCategoryActionEventService,
                )

            Mockito.`when`(repository.findOneByIdAndUserId(categoryId, userId)).thenReturn(Mono.just(category))

            assertThatThrownBy {
                runBlocking { service.deleteCategory(userId = userId, id = categoryId) }
            }.isInstanceOf(DebtSfCategoryProtectedException::class.java)

            Mockito.verify(repository, Mockito.never()).deleteByIdAndUserId(categoryId, userId)
        }

    @Test
    fun `user deleteCategory should cleanup orphaned custom concept after successful delete`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val customConceptId = UUID.randomUUID()
            val category = category(id = categoryId, userId = userId, conceptId = customConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val categoryConceptService = FakeCategoryConceptService(debtConceptId = debtConceptId)
            val service =
                UserCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService = categoryConceptService,
                    databaseHelperService = NoOpDatabaseHelperService,
                    userCategoryActionEventService = NoOpUserCategoryActionEventService,
                )

            Mockito.`when`(repository.findOneByIdAndUserId(categoryId, userId)).thenReturn(Mono.just(category))
            Mockito.`when`(repository.deleteByIdAndUserId(categoryId, userId)).thenReturn(Mono.just(1))

            val deleted = service.deleteCategory(userId = userId, id = categoryId)

            assertThat(deleted).isTrue()
            assertThat(categoryConceptService.cleanedConceptIds).containsExactly(customConceptId)
        }

    @Test
    fun `user editCategory should reject rebinding DEBT_SF category concept`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val supermarketConceptId = UUID.randomUUID()
            val category = category(id = categoryId, userId = userId, conceptId = debtConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                UserCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService =
                        FakeCategoryConceptService(
                            debtConceptId = debtConceptId,
                            additionalConceptIds = setOf(supermarketConceptId),
                        ),
                    databaseHelperService = NoOpDatabaseHelperService,
                    userCategoryActionEventService = NoOpUserCategoryActionEventService,
                )

            Mockito.`when`(repository.findOneByIdAndUserId(categoryId, userId)).thenReturn(Mono.just(category))

            assertThatThrownBy {
                runBlocking {
                    service.editCategory(
                        userId = userId,
                        id = categoryId,
                        editCategory =
                            EditCategoryRequest(
                                name = "Debt SF renamed",
                                color = "#f31261",
                                parentId = null,
                                conceptId = supermarketConceptId,
                            ),
                    )
                }
            }.isInstanceOf(DebtSfCategoryProtectedException::class.java)

            Mockito
                .verify(repository, Mockito.never())
                .updateByUserId(
                    Mockito.eq(categoryId),
                    Mockito.eq(userId),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.isNull(),
                    Mockito.any(UUID::class.java),
                )
        }

    @Test
    fun `user newCategory should reject duplicate concept in same owner scope`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val targetConceptId = UUID.randomUUID()
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                UserCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService =
                        FakeCategoryConceptService(
                            debtConceptId = debtConceptId,
                            additionalConceptIds = setOf(targetConceptId),
                        ),
                    databaseHelperService = NoOpDatabaseHelperService,
                    userCategoryActionEventService = NoOpUserCategoryActionEventService,
                )

            Mockito
                .`when`(repository.findAllByUserIdAndConceptId(userId, targetConceptId))
                .thenReturn(Flux.just(category(id = UUID.randomUUID(), userId = userId, conceptId = targetConceptId)))

            assertThatThrownBy {
                runBlocking {
                    service.newCategory(
                        userId = userId,
                        newCategoryRequest =
                            NewCategoryRequest(
                                name = "Supermarket",
                                color = "#00FF00",
                                parentId = null,
                                conceptId = targetConceptId,
                            ),
                    )
                }
            }.isInstanceOf(DuplicatedCategoryConceptException::class.java)
        }

    @Test
    fun `group deleteCategory should reject DEBT_SF category`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val category = category(id = categoryId, groupId = groupId, conceptId = debtConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                GroupCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService = FakeCategoryConceptService(debtConceptId = debtConceptId),
                    databaseHelperService = NoOpDatabaseHelperService,
                    groupCategoryActionEventService = NoOpGroupCategoryActionEventService,
                    groupPermissionService = AllowAllGroupPermissionService,
                )

            Mockito.`when`(repository.findOneByIdAndGroupId(categoryId, groupId)).thenReturn(Mono.just(category))

            assertThatThrownBy {
                runBlocking { service.deleteCategory(userId = userId, groupId = groupId, id = categoryId) }
            }.isInstanceOf(DebtSfCategoryProtectedException::class.java)

            Mockito.verify(repository, Mockito.never()).deleteByIdAndGroupId(categoryId, groupId)
        }

    @Test
    fun `group deleteCategory should cleanup orphaned custom concept after successful delete`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val customConceptId = UUID.randomUUID()
            val category = category(id = categoryId, groupId = groupId, conceptId = customConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val categoryConceptService = FakeCategoryConceptService(debtConceptId = debtConceptId)
            val service =
                GroupCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService = categoryConceptService,
                    databaseHelperService = NoOpDatabaseHelperService,
                    groupCategoryActionEventService = NoOpGroupCategoryActionEventService,
                    groupPermissionService = AllowAllGroupPermissionService,
                )

            Mockito.`when`(repository.findOneByIdAndGroupId(categoryId, groupId)).thenReturn(Mono.just(category))
            Mockito.`when`(repository.deleteByIdAndGroupId(categoryId, groupId)).thenReturn(Mono.just(1))

            val deleted = service.deleteCategory(userId = userId, groupId = groupId, id = categoryId)

            assertThat(deleted).isTrue()
            assertThat(categoryConceptService.cleanedConceptIds).containsExactly(customConceptId)
        }

    @Test
    fun `group editCategory should reject rebinding regular category to DEBT_SF concept`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val categoryId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val nonDebtConceptId = UUID.randomUUID()
            val category = category(id = categoryId, groupId = groupId, conceptId = nonDebtConceptId)
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                GroupCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService =
                        FakeCategoryConceptService(
                            debtConceptId = debtConceptId,
                            additionalConceptIds = setOf(nonDebtConceptId),
                        ),
                    databaseHelperService = NoOpDatabaseHelperService,
                    groupCategoryActionEventService = NoOpGroupCategoryActionEventService,
                    groupPermissionService = AllowAllGroupPermissionService,
                )

            Mockito.`when`(repository.findOneByIdAndGroupId(categoryId, groupId)).thenReturn(Mono.just(category))

            assertThatThrownBy {
                runBlocking {
                    service.editCategory(
                        userId = userId,
                        groupId = groupId,
                        id = categoryId,
                        editCategory =
                            EditCategoryRequest(
                                name = "General",
                                color = "#111111",
                                parentId = null,
                                conceptId = debtConceptId,
                            ),
                    )
                }
            }.isInstanceOf(DebtSfCategoryProtectedException::class.java)

            Mockito
                .verify(repository, Mockito.never())
                .updateByGroupId(
                    Mockito.eq(categoryId),
                    Mockito.eq(groupId),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.isNull(),
                    Mockito.any(UUID::class.java),
                )
        }

    @Test
    fun `group newCategory should reject duplicate concept in same owner scope`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val debtConceptId = UUID.fromString("00000000-0000-0000-0000-00000000debf")
            val targetConceptId = UUID.randomUUID()
            val repository = mock(WalletEntryCategoryRepository::class.java)
            val service =
                GroupCategoryServiceImpl(
                    repository = repository,
                    categoryConceptService =
                        FakeCategoryConceptService(
                            debtConceptId = debtConceptId,
                            additionalConceptIds = setOf(targetConceptId),
                        ),
                    databaseHelperService = NoOpDatabaseHelperService,
                    groupCategoryActionEventService = NoOpGroupCategoryActionEventService,
                    groupPermissionService = AllowAllGroupPermissionService,
                )

            Mockito
                .`when`(repository.findAllByGroupIdAndConceptId(groupId, targetConceptId))
                .thenReturn(Flux.just(category(id = UUID.randomUUID(), groupId = groupId, conceptId = targetConceptId)))

            assertThatThrownBy {
                runBlocking {
                    service.newCategory(
                        userId = userId,
                        groupId = groupId,
                        newCategoryRequest =
                            NewCategoryRequest(
                                name = "Travel",
                                color = "#123456",
                                parentId = null,
                                conceptId = targetConceptId,
                            ),
                    )
                }
            }.isInstanceOf(DuplicatedCategoryConceptException::class.java)
        }

    private fun category(
        id: UUID,
        conceptId: UUID,
        userId: UUID? = null,
        groupId: UUID? = null,
    ) = WalletEntryCategoryEntity(
        name = "Category",
        color = "#ffffff",
        userId = userId,
        groupId = groupId,
        parentId = null,
        conceptId = conceptId,
    ).also { it.id = id }

    private object NoOpDatabaseHelperService : DatabaseHelperService {
        override fun isUniqueViolation(
            t: Throwable,
            indexName: String,
        ): Boolean = false
    }

    private object NoOpUserCategoryActionEventService : UserCategoryActionEventService {
        override suspend fun sendInsertedCategory(
            userId: UUID,
            category: WalletEntryCategoryEntity,
        ) = Unit

        override suspend fun sendUpdatedCategory(
            userId: UUID,
            category: WalletEntryCategoryEntity,
        ) = Unit

        override suspend fun sendDeletedCategory(
            userId: UUID,
            id: UUID,
        ) = Unit
    }

    private object NoOpGroupCategoryActionEventService : GroupCategoryActionEventService {
        override suspend fun sendInsertedCategory(
            userId: UUID,
            category: WalletEntryCategoryEntity,
        ) = Unit

        override suspend fun sendUpdatedCategory(
            userId: UUID,
            category: WalletEntryCategoryEntity,
        ) = Unit

        override suspend fun sendDeletedCategory(
            userId: UUID,
            groupId: UUID,
            id: UUID,
        ) = Unit
    }

    private object AllowAllGroupPermissionService : GroupPermissionService {
        override suspend fun hasPermission(
            userId: UUID,
            groupId: UUID,
            permission: GroupPermissions?,
        ): Boolean = true

        override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> = emptySet()
    }

    private class FakeCategoryConceptService(
        debtConceptId: UUID,
        additionalConceptIds: Set<UUID> = emptySet(),
    ) : CategoryConceptService {
        val cleanedConceptIds = mutableListOf<UUID>()

        private val conceptsById: Map<UUID, WalletCategoryConceptEntity> =
            buildMap {
                put(
                    debtConceptId,
                    concept(
                        id = debtConceptId,
                        kind = WalletCategoryConceptKind.PREDEFINED,
                        code = WalletCategoryConceptCode.DEBT_SF,
                    ),
                )
                additionalConceptIds.forEach { conceptId ->
                    put(
                        conceptId,
                        concept(
                            id = conceptId,
                            kind = WalletCategoryConceptKind.PREDEFINED,
                            code = WalletCategoryConceptCode.SUPERMARKET,
                        ),
                    )
                }
            }

        override suspend fun findById(id: UUID): WalletCategoryConceptEntity? = conceptsById[id]

        override suspend fun findRequiredByCode(code: WalletCategoryConceptCode): WalletCategoryConceptEntity {
            check(code == WalletCategoryConceptCode.DEBT_SF)
            return conceptsById.values.first { it.code == WalletCategoryConceptCode.DEBT_SF }
        }

        override suspend fun listAvailableForUser(userId: UUID): List<WalletCategoryConceptEntity> = conceptsById.values.toList()

        override suspend fun resolveForMutation(
            conceptId: UUID?,
            customConceptName: String?,
        ): WalletCategoryConceptEntity =
            when {
                conceptId != null -> requireNotNull(conceptsById[conceptId]) { "Unknown concept id $conceptId" }
                !customConceptName.isNullOrBlank() ->
                    concept(
                        id = UUID.randomUUID(),
                        kind = WalletCategoryConceptKind.CUSTOM,
                        code = null,
                        displayName = customConceptName,
                    )
                else -> conceptsById.values.first { it.code == WalletCategoryConceptCode.DEBT_SF }
            }

        override suspend fun cleanupOrphanedCustomConcept(conceptId: UUID): Boolean {
            cleanedConceptIds += conceptId
            return true
        }

        private fun concept(
            id: UUID,
            kind: WalletCategoryConceptKind,
            code: WalletCategoryConceptCode?,
            displayName: String? = null,
        ) = WalletCategoryConceptEntity(
            kind = kind,
            code = code,
            displayName = displayName,
        ).also { it.id = id }
    }
}
