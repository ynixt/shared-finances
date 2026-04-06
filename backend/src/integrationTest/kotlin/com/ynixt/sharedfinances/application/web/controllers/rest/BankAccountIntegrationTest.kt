package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class BankAccountIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var userTestUtil: UserTestUtil

    @BeforeEach
    fun setup() {
        userTestUtil =
            UserTestUtil(
                webClient = webClient,
                passwordEncoder = passwordEncoder,
                userRepository = userRepository,
            )
    }

    @Test
    fun `should create a new bank account for authenticated user`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val requestJson = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")
            val newBankAccountDto = objectMapper.readValue<NewBankAccountDto>(requestJson)

            webClient
                .post()
                .uri("/bank-accounts")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.id")
                .isNotEmpty
                .jsonPath("$.name")
                .isEqualTo(newBankAccountDto.name)
                .jsonPath("$.currency")
                .isEqualTo(newBankAccountDto.currency)
                .jsonPath("$.balance")
                .isNotEmpty

            val savedWalletItems =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = user.id!!,
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = PageRequest.of(0, 10),
                    ).collectList()
                    .awaitSingle()

            assertThat(savedWalletItems).hasSize(1)

            val savedBankAccount = savedWalletItems.first()
            val expectedBalance = requireNotNull(newBankAccountDto.balance)

            assertThat(savedBankAccount.name).isEqualTo(newBankAccountDto.name)
            assertThat(savedBankAccount.currency).isEqualTo(newBankAccountDto.currency)
            assertThat(savedBankAccount.balance).isEqualByComparingTo(expectedBalance)
            assertThat(savedBankAccount.totalLimit).isNull()
            assertThat(savedBankAccount.dueDay).isNull()
            assertThat(savedBankAccount.daysBetweenDueAndClosing).isNull()
            assertThat(savedBankAccount.dueOnNextBusinessDay).isNull()
        }
    }
}
