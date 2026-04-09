package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import com.ynixt.sharedfinances.support.IntegrationTestContainers
import com.ynixt.sharedfinances.support.util.JsonUtil
import com.ynixt.sharedfinances.support.util.UserTestUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class WalletEntryIntegrationTest : IntegrationTestContainers() {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletItemRepository: WalletItemRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

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
    fun `should create installment transaction without fk violation`() {
        runBlocking {
            val user = userTestUtil.createUserOnDatabase()
            val accessToken = userTestUtil.login()

            val bankAccountRequestJson = JsonUtil.readJsonFromResources("mocks/bank-account/new-bank-account-request-200.json")

            webClient
                .post()
                .uri("/bank-accounts")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bankAccountRequestJson)
                .exchange()
                .expectStatus()
                .isOk

            val bankAccountId =
                walletItemRepository
                    .findAllByUserIdAndType(
                        userId = user.id!!,
                        type = WalletItemType.BANK_ACCOUNT,
                        pageable = PageRequest.of(0, 1),
                    ).collectList()
                    .awaitSingle()
                    .single()
                    .id!!

            webClient
                .post()
                .uri("/wallet-entries")
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    mapOf(
                        "type" to "EXPENSE",
                        "groupId" to null,
                        "originId" to bankAccountId,
                        "targetId" to null,
                        "name" to "Installment Purchase",
                        "categoryId" to null,
                        "date" to LocalDate.now(),
                        "value" to BigDecimal("300.00"),
                        "confirmed" to true,
                        "observations" to null,
                        "paymentType" to "INSTALLMENTS",
                        "installments" to 3,
                        "periodicity" to "MONTHLY",
                        "periodicityQtyLimit" to null,
                        "originBillDate" to null,
                        "targetBillDate" to null,
                        "tags" to listOf("integration-test"),
                    ),
                ).exchange()
                .expectStatus()
                .isNoContent
        }
    }
}
