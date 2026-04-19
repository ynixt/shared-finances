package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.OnlyIdDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupDto
import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.NewGroupDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.UserTestUtil
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
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GroupWalletAssociationOwnershipIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var ownerUserTestUtil: UserTestUtil
    private lateinit var memberUserTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        ownerUserTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
        memberUserTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `allowed association endpoints should return only requester-owned unassociated items`() =
        runBlocking {
            val ownerUser = ownerUserTestUtil.createUserOnDatabase()
            memberUserTestUtil.createUserOnDatabase()
            val ownerToken = ownerUserTestUtil.login()
            val memberToken = memberUserTestUtil.login()
            val group = createGroup(ownerToken, "Ownership filter group")

            val invite = generateInvitation(ownerToken, group.id)
            acceptInvitation(memberToken, invite.id)

            val ownerAssociatedBankId = createBankAccount(ownerToken, "Owner associated bank")
            val ownerAllowedBankId = createBankAccount(ownerToken, "Owner allowed bank")
            createBankAccount(memberToken, "Member bank")

            associateBank(ownerToken, group.id, ownerAssociatedBankId)

            val allowedBanks = getAllowedBanks(ownerToken, group.id)
            assertThat(allowedBanks).extracting<UUID> { it.id }.containsExactly(ownerAllowedBankId)
            assertThat(allowedBanks).allMatch { it.user.id == ownerUser.id!! }

            val ownerAssociatedCreditCardId = createCreditCard(ownerToken, "Owner associated card")
            val ownerAllowedCreditCardId = createCreditCard(ownerToken, "Owner allowed card")
            createCreditCard(memberToken, "Member card")

            associateCreditCard(ownerToken, group.id, ownerAssociatedCreditCardId)

            val allowedCreditCards = getAllowedCreditCards(ownerToken, group.id)
            assertThat(allowedCreditCards).extracting<UUID> { it.id }.containsExactly(ownerAllowedCreditCardId)
            assertThat(allowedCreditCards).allMatch { it.user.id == ownerUser.id!! }
        }

    @Test
    fun `association should reject other user item with forbidden and keep not-found for missing id`() =
        runBlocking {
            ownerUserTestUtil.createUserOnDatabase()
            memberUserTestUtil.createUserOnDatabase()
            val ownerToken = ownerUserTestUtil.login()
            val memberToken = memberUserTestUtil.login()
            val group = createGroup(ownerToken, "Ownership guard group")

            val invite = generateInvitation(ownerToken, group.id)
            acceptInvitation(memberToken, invite.id)

            val memberBankId = createBankAccount(memberToken, "Member guarded bank")
            val memberCreditCardId = createCreditCard(memberToken, "Member guarded card")

            webClient
                .put()
                .uri("/groups/${group.id}/associations/banks/$memberBankId")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isForbidden

            webClient
                .put()
                .uri("/groups/${group.id}/associations/creditCards/$memberCreditCardId")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isForbidden

            webClient
                .put()
                .uri("/groups/${group.id}/associations/banks/${UUID.randomUUID()}")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isNotFound

            webClient
                .put()
                .uri("/groups/${group.id}/associations/creditCards/${UUID.randomUUID()}")
                .header(HttpHeaders.AUTHORIZATION, ownerToken)
                .exchange()
                .expectStatus()
                .isNotFound
        }

    @Test
    fun `association should keep success behavior for requester-owned items`() =
        runBlocking {
            ownerUserTestUtil.createUserOnDatabase()
            val ownerToken = ownerUserTestUtil.login()
            val group = createGroup(ownerToken, "Owner association group")
            val ownerBankId = createBankAccount(ownerToken, "Owner bank")
            val ownerCreditCardId = createCreditCard(ownerToken, "Owner card")

            associateBank(ownerToken, group.id, ownerBankId)
            associateCreditCard(ownerToken, group.id, ownerCreditCardId)

            val associatedBanks = getAssociatedBanks(ownerToken, group.id)
            assertThat(associatedBanks).extracting<UUID> { it.id }.contains(ownerBankId)

            val associatedCreditCards = getAssociatedCreditCards(ownerToken, group.id)
            assertThat(associatedCreditCards).extracting<UUID> { it.id }.contains(ownerCreditCardId)
        }

    private suspend fun createGroup(
        accessToken: String,
        name: String,
    ): GroupDto =
        webClient
            .post()
            .uri("/groups")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(NewGroupDto(name = name, categories = null))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun generateInvitation(
        accessToken: String,
        groupId: UUID,
    ): GroupInviteDto =
        webClient
            .post()
            .uri("/groups/$groupId/members/generate-invitation")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(GroupInviteDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun acceptInvitation(
        accessToken: String,
        inviteId: UUID,
    ): OnlyIdDto =
        webClient
            .put()
            .uri("/group-invite/$inviteId/accept")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(OnlyIdDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun createBankAccount(
        accessToken: String,
        name: String,
    ): UUID =
        webClient
            .post()
            .uri("/bank-accounts")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                NewBankAccountDto(
                    name = name,
                    balance = BigDecimal("100.00"),
                    currency = "BRL",
                    showOnDashboard = true,
                ),
            ).exchange()
            .expectStatus()
            .isOk
            .expectBody(BankAccountDto::class.java)
            .returnResult()
            .responseBody!!
            .id

    private suspend fun createCreditCard(
        accessToken: String,
        name: String,
    ): UUID =
        webClient
            .post()
            .uri("/credit-cards")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                NewCreditCardDto(
                    name = name,
                    currency = "BRL",
                    totalLimit = BigDecimal("500.00"),
                    dueDay = 15,
                    daysBetweenDueAndClosing = 7,
                    dueOnNextBusinessDay = true,
                    showOnDashboard = true,
                ),
            ).exchange()
            .expectStatus()
            .isOk
            .expectBody(CreditCardDto::class.java)
            .returnResult()
            .responseBody!!
            .id

    private suspend fun associateBank(
        accessToken: String,
        groupId: UUID,
        bankAccountId: UUID,
    ) {
        webClient
            .put()
            .uri("/groups/$groupId/associations/banks/$bankAccountId")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    private suspend fun associateCreditCard(
        accessToken: String,
        groupId: UUID,
        creditCardId: UUID,
    ) {
        webClient
            .put()
            .uri("/groups/$groupId/associations/creditCards/$creditCardId")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    private suspend fun getAllowedBanks(
        accessToken: String,
        groupId: UUID,
    ): List<BankAccountForGroupAssociateDto> =
        webClient
            .get()
            .uri("/groups/$groupId/associations/banks/allowed")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BankAccountForGroupAssociateDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getAllowedCreditCards(
        accessToken: String,
        groupId: UUID,
    ): List<CreditCardForGroupAssociateDto> =
        webClient
            .get()
            .uri("/groups/$groupId/associations/creditCards/allowed")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(CreditCardForGroupAssociateDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getAssociatedBanks(
        accessToken: String,
        groupId: UUID,
    ): List<BankAccountForGroupAssociateDto> =
        webClient
            .get()
            .uri("/groups/$groupId/associations/banks")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(BankAccountForGroupAssociateDto::class.java)
            .returnResult()
            .responseBody!!

    private suspend fun getAssociatedCreditCards(
        accessToken: String,
        groupId: UUID,
    ): List<CreditCardForGroupAssociateDto> =
        webClient
            .get()
            .uri("/groups/$groupId/associations/creditCards")
            .header(HttpHeaders.AUTHORIZATION, accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(CreditCardForGroupAssociateDto::class.java)
            .returnResult()
            .responseBody!!
}
