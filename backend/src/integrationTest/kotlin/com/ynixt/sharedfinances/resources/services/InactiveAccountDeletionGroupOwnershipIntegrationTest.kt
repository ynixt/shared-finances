package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.InactiveAccountDeletionService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["app.inactive-account-deletion.enabled=true"])
class InactiveAccountDeletionGroupOwnershipIntegrationTest : IntegrationTestContainers() {
    @Autowired private lateinit var service: InactiveAccountDeletionService

    @Autowired private lateinit var users: UserRepository

    @Autowired private lateinit var groups: GroupRepository

    @Autowired private lateinit var groupUsers: GroupUsersRepository

    @MockitoSpyBean private lateinit var groupEvents: GroupActionEventService

    @Test
    fun `dormant owner deletion removes owned group and notifies its former members`() =
        runBlocking {
            val owner = insertUser("owner", OffsetDateTime.now().minusMonths(13), noticeStage = 1)
            val member = insertUser("member", OffsetDateTime.now(), noticeStage = null)
            val group = groups.save(GroupEntity("Dormant owner's group", owner.id!!)).awaitSingle()
            groupUsers.save(GroupUserEntity(group.id!!, owner.id!!, UserGroupRole.ADMIN)).awaitSingle()
            groupUsers.save(GroupUserEntity(group.id!!, member.id!!, UserGroupRole.EDITOR)).awaitSingle()

            val result = service.runCleanup()

            assertThat(result?.accountsDeleted).isEqualTo(1)
            assertThat(users.existsById(owner.id!!).awaitSingle()).isFalse()
            assertThat(groups.existsById(group.id!!).awaitSingle()).isFalse()
            val notification =
                Mockito
                    .mockingDetails(groupEvents)
                    .invocations
                    .single { it.method.name == "sendDeletedGroup" }
            assertThat(notification.arguments[0]).isEqualTo(owner.id)
            assertThat(notification.arguments[1]).isEqualTo(group.id)
            @Suppress("UNCHECKED_CAST")
            val notifiedMembers = notification.arguments[2] as List<UUID>
            assertThat(notifiedMembers).containsExactlyInAnyOrder(owner.id, member.id)
        }

    private suspend fun insertUser(
        prefix: String,
        lastLoginAt: OffsetDateTime,
        noticeStage: Int?,
    ): UserEntity =
        users
            .insert(
                UserEntity(
                    email = "$prefix-${UUID.randomUUID()}@example.com",
                    passwordHash = "hash",
                    firstName = prefix,
                    lastName = "User",
                    lang = "en-US",
                    defaultCurrency = "USD",
                    tmz = "UTC",
                    photoUrl = null,
                    emailVerified = true,
                    mfaEnabled = false,
                    totpSecret = null,
                    onboardingDone = true,
                    role = UserPlanRole.USER,
                    lastLoginAt = lastLoginAt,
                    inactivityNoticeStage = noticeStage,
                ),
            ).awaitSingle()
}
