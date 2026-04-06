package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
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
class CreditCardIntegrationTest : IntegrationTestContainers() {
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
    fun `should create a new credit card for authenticated user`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()
            val requestJson = JsonUtil.readJsonFromResources("mocks/credit-card/new-credit-card-request-200.json")
            val newCreditCardDto = objectMapper.readValue<NewCreditCardDto>(requestJson)

            webClient
                .post()
                .uri("/credit-cards")
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
                .isEqualTo(newCreditCardDto.name)
                .jsonPath("$.currency")
                .isEqualTo(newCreditCardDto.currency)
                .jsonPath("$.dueDay")
                .isEqualTo(newCreditCardDto.dueDay)
                .jsonPath("$.daysBetweenDueAndClosing")
                .isEqualTo(newCreditCardDto.daysBetweenDueAndClosing)
                .jsonPath("$.dueOnNextBusinessDay")
                .isEqualTo(newCreditCardDto.dueOnNextBusinessDay)

            val savedWalletItems =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = user.id!!,
                        type = WalletItemType.CREDIT_CARD,
                        pageable = PageRequest.of(0, 10),
                    ).collectList()
                    .awaitSingle()

            assertThat(savedWalletItems).hasSize(1)

            val savedCard = savedWalletItems.first()

            assertThat(savedCard.name).isEqualTo(newCreditCardDto.name)
            assertThat(savedCard.currency).isEqualTo(newCreditCardDto.currency)
            assertThat(savedCard.totalLimit).isEqualByComparingTo(newCreditCardDto.totalLimit)
            assertThat(savedCard.balance).isEqualByComparingTo(newCreditCardDto.totalLimit)
            assertThat(savedCard.dueDay).isEqualTo(newCreditCardDto.dueDay)
            assertThat(savedCard.daysBetweenDueAndClosing).isEqualTo(newCreditCardDto.daysBetweenDueAndClosing)
            assertThat(savedCard.dueOnNextBusinessDay).isEqualTo(newCreditCardDto.dueOnNextBusinessDay)
        }
    }
}
