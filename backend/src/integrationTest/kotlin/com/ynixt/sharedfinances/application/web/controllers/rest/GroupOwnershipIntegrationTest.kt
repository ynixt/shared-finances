package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.OnlyIdDto
import com.ynixt.sharedfinances.application.web.dto.groups.ChangeRoleGroupUserRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupWithRoleDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.TransferGroupOwnershipDto
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GroupOwnershipIntegrationTest : IntegrationTestContainers() {
    @Autowired private lateinit var webClient: WebTestClient

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var passwordEncoder: PasswordEncoder

    @Autowired private lateinit var groupRepository: GroupRepository

    @Autowired private lateinit var groupUsersRepository: GroupUsersRepository

    private lateinit var ownerUtil: UserTestUtil
    private lateinit var adminUtil: UserTestUtil
    private lateinit var viewerUtil: UserTestUtil
    private lateinit var outsiderUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        ownerUtil = testUser()
        adminUtil = testUser()
        viewerUtil = testUser()
        outsiderUtil = testUser()
    }

    @Test
    fun `single and list endpoints expose ownership to owner admin and viewer`() =
        runBlocking {
            val fixture = fixture()

            assertOwnership(getGroup(fixture.ownerToken, fixture.groupId), fixture.ownerId, true)
            assertOwnership(getGroups(fixture.ownerToken).single { it.id == fixture.groupId }, fixture.ownerId, true)

            assertOwnership(getGroup(fixture.adminToken, fixture.groupId), fixture.ownerId, false)
            assertOwnership(getGroups(fixture.adminToken).single { it.id == fixture.groupId }, fixture.ownerId, false)

            assertOwnership(getGroup(fixture.viewerToken, fixture.groupId), fixture.ownerId, false)
            assertOwnership(getGroups(fixture.viewerToken).single { it.id == fixture.groupId }, fixture.ownerId, false)
        }

    @Test
    fun `group deletion is owner-only and keeps not-found response for a non-member`() =
        runBlocking {
            val fixture = fixture()

            webClient
                .delete()
                .uri("/groups/${fixture.groupId}")
                .header(HttpHeaders.AUTHORIZATION, fixture.adminToken)
                .exchange()
                .expectStatus()
                .isForbidden
            assertThat(groupRepository.existsById(fixture.groupId).awaitSingle()).isTrue()

            webClient
                .delete()
                .uri("/groups/${fixture.groupId}")
                .header(HttpHeaders.AUTHORIZATION, fixture.outsiderToken)
                .exchange()
                .expectStatus()
                .isNotFound
            assertThat(groupRepository.existsById(fixture.groupId).awaitSingle()).isTrue()

            webClient
                .delete()
                .uri("/groups/${fixture.groupId}")
                .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                .exchange()
                .expectStatus()
                .isNoContent
            assertThat(groupRepository.existsById(fixture.groupId).awaitSingle()).isFalse()
        }

    @Test
    fun `transfer endpoint persists new owner promotes destination and keeps previous owner admin`() =
        runBlocking {
            val fixture = fixture()

            val response =
                webClient
                    .put()
                    .uri("/groups/${fixture.groupId}/owner")
                    .header(HttpHeaders.AUTHORIZATION, fixture.ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(TransferGroupOwnershipDto(fixture.viewerId))
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(GroupWithRoleDto::class.java)
                    .returnResult()
                    .responseBody!!

            assertThat(response.ownerUserId).isEqualTo(fixture.viewerId)
            assertThat(response.isOwner).isFalse()
            assertThat(groupRepository.findById(fixture.groupId).awaitSingle().ownerUserId).isEqualTo(fixture.viewerId)
            assertThat(groupUsersRepository.findOneByGroupIdAndUserId(fixture.groupId, fixture.viewerId).awaitSingle().role)
                .isEqualTo(UserGroupRole.ADMIN)
            assertThat(groupUsersRepository.findOneByGroupIdAndUserId(fixture.groupId, fixture.ownerId).awaitSingle().role)
                .isEqualTo(UserGroupRole.ADMIN)
        }

    private suspend fun fixture(): Fixture {
        val owner = ownerUtil.createUserOnDatabase()
        val admin = adminUtil.createUserOnDatabase()
        val viewer = viewerUtil.createUserOnDatabase()
        outsiderUtil.createUserOnDatabase()
        val ownerToken = ownerUtil.login()
        val adminToken = adminUtil.login()
        val viewerToken = viewerUtil.login()
        val outsiderToken = outsiderUtil.login()
        val group = createGroup(ownerToken)
        acceptInvitation(adminToken, generateInvitation(ownerToken, group.id).id)
        acceptInvitation(viewerToken, generateInvitation(ownerToken, group.id).id)
        changeRole(ownerToken, group.id, admin.id!!, UserGroupRole.ADMIN)
        return Fixture(group.id, owner.id!!, viewer.id!!, ownerToken, adminToken, viewerToken, outsiderToken)
    }

    private suspend fun createGroup(token: String): GroupDto =
        webClient
            .post()
            .uri("/groups")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(NewGroupDto("Owned group", null))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun generateInvitation(
        token: String,
        groupId: UUID,
    ): GroupInviteDto =
        webClient
            .post()
            .uri("/groups/$groupId/members/generate-invitation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupInviteDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun acceptInvitation(
        token: String,
        inviteId: UUID,
    ): OnlyIdDto =
        webClient
            .put()
            .uri("/group-invite/$inviteId/accept")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OnlyIdDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun changeRole(
        token: String,
        groupId: UUID,
        memberId: UUID,
        role: UserGroupRole,
    ) {
        webClient
            .put()
            .uri("/groups/$groupId/members/change-role")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ChangeRoleGroupUserRequestDto(memberId, role))
            .exchange()
            .expectStatus()
            .isNoContent
    }

    private suspend fun getGroup(
        token: String,
        groupId: UUID,
    ): GroupWithRoleDto =
        webClient
            .get()
            .uri("/groups/$groupId")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupWithRoleDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getGroups(token: String): List<GroupWithRoleDto> =
        webClient
            .get()
            .uri("/groups")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(GroupWithRoleDto::class.java)
            .returnResult()
            .responseBody!!

    private fun assertOwnership(
        group: GroupWithRoleDto,
        ownerId: UUID,
        isOwner: Boolean,
    ) {
        assertThat(group.ownerUserId).isEqualTo(ownerId)
        assertThat(group.isOwner).isEqualTo(isOwner)
    }

    private fun testUser() = UserTestUtil(userRepository, passwordEncoder, webClient)

    private data class Fixture(
        val groupId: UUID,
        val ownerId: UUID,
        val viewerId: UUID,
        val ownerToken: String,
        val adminToken: String,
        val viewerToken: String,
        val outsiderToken: String,
    )
}
