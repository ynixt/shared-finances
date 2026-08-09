package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class AccountDeletionGroupOwnershipIntegrationTest : IntegrationTestContainers() {
    @Autowired private lateinit var accountDeletionService: AccountDeletionService

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var groupRepository: GroupRepository

    @Autowired private lateinit var groupUsersRepository: GroupUsersRepository

    @Autowired private lateinit var dbClient: DatabaseClient

    @Test
    fun `deleting an owner removes group and dependent rows before restricted user deletion`() =
        runBlocking {
            val user =
                userRepository
                    .insert(
                        UserEntity(
                            email = "owner-${UUID.randomUUID()}@example.com",
                            passwordHash = "hash",
                            firstName = "Owner",
                            lastName = "User",
                            lang = "en-US",
                            defaultCurrency = "BRL",
                            tmz = "UTC",
                            photoUrl = null,
                            emailVerified = true,
                            mfaEnabled = false,
                            totpSecret = null,
                            onboardingDone = true,
                        ),
                    ).awaitSingle()
            val ownerId = user.id!!
            val group = groupRepository.save(GroupEntity("Owned", ownerId)).awaitSingle()
            groupUsersRepository.save(GroupUserEntity(group.id!!, ownerId, UserGroupRole.ADMIN)).awaitSingle()
            val inviteId = UUID.randomUUID()
            dbClient
                .sql("INSERT INTO group_invite(id, group_id, expire_at) VALUES (:id, :groupId, :expireAt)")
                .bind("id", inviteId)
                .bind("groupId", group.id!!)
                .bind("expireAt", OffsetDateTime.now().plusHours(1))
                .fetch()
                .rowsUpdated()
                .awaitSingle()

            accountDeletionService.deleteAccountForUser(ownerId)

            assertThat(userRepository.existsById(ownerId).awaitSingle()).isFalse()
            assertThat(groupRepository.existsById(group.id!!).awaitSingle()).isFalse()
            val inviteCount =
                dbClient
                    .sql("SELECT COUNT(*) AS qty FROM group_invite WHERE id = :id")
                    .bind("id", inviteId)
                    .map { row, _ -> row.get("qty", java.lang.Long::class.java)!!.toLong() }
                    .one()
                    .awaitSingle()
            assertThat(inviteCount).isZero()
            Unit
        }
}
