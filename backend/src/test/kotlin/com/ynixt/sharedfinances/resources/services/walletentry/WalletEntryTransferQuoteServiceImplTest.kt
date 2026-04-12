package com.ynixt.sharedfinances.resources.services.walletentry

import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.TransferQuoteRequest
import com.ynixt.sharedfinances.domain.services.walletentry.TransferRateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class WalletEntryTransferQuoteServiceImplTest {
    @Test
    fun `should mirror origin amount when transfer currencies match`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val originId = UUID.randomUUID()
            val targetId = UUID.randomUUID()
            val origin = bankAccount(id = originId, userId = userId, currency = "USD")
            val target = bankAccount(id = targetId, userId = userId, currency = "USD")

            val service =
                WalletEntryTransferQuoteServiceImpl(
                    walletItemService = FakeWalletItemService(origin, target),
                    groupService = NoOpGroupService(),
                    exchangeRateService =
                        object : ExchangeRateService {
                            override suspend fun syncLatestQuotes(): Int = 0

                            override suspend fun syncQuotesForDate(
                                date: LocalDate,
                                baseCurrencies: Set<String>?,
                            ): Int = 0

                            override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
                                CursorPage(items = emptyList(), nextCursor = null, hasNext = false)

                            override suspend fun getRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = BigDecimal.ONE

                            override suspend fun resolveRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): ResolvedExchangeRate = ResolvedExchangeRate(rate = BigDecimal.ONE, quoteDate = referenceDate)

                            override suspend fun convert(
                                value: BigDecimal,
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal {
                                error("Same-currency quote should not call exchange rate conversion")
                            }

                            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                                requests.associateWith { it.value }
                        },
                )

            val result =
                service.quote(
                    userId = userId,
                    request =
                        TransferQuoteRequest(
                            originId = originId,
                            targetId = targetId,
                            date = LocalDate.of(2026, 4, 10),
                            originValue = BigDecimal("123.45"),
                        ),
                )

            assertThat(result.targetValue).isEqualByComparingTo(BigDecimal("123.45"))
        }

    @Test
    fun `should convert origin amount when transfer currencies differ`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val originId = UUID.randomUUID()
            val targetId = UUID.randomUUID()
            val origin = bankAccount(id = originId, userId = userId, currency = "USD")
            val target = bankAccount(id = targetId, userId = userId, currency = "BRL")
            var capturedRequest: ConversionRequest? = null

            val service =
                WalletEntryTransferQuoteServiceImpl(
                    walletItemService = FakeWalletItemService(origin, target),
                    groupService = NoOpGroupService(),
                    exchangeRateService =
                        object : ExchangeRateService {
                            override suspend fun syncLatestQuotes(): Int = 0

                            override suspend fun syncQuotesForDate(
                                date: LocalDate,
                                baseCurrencies: Set<String>?,
                            ): Int = 0

                            override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
                                CursorPage(items = emptyList(), nextCursor = null, hasNext = false)

                            override suspend fun getRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = BigDecimal("5.40")

                            override suspend fun resolveRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): ResolvedExchangeRate = ResolvedExchangeRate(rate = BigDecimal("5.40"), quoteDate = LocalDate.of(2026, 4, 9))

                            override suspend fun convert(
                                value: BigDecimal,
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal {
                                capturedRequest =
                                    ConversionRequest(
                                        value = value,
                                        fromCurrency = fromCurrency,
                                        toCurrency = toCurrency,
                                        referenceDate = referenceDate,
                                    )
                                return BigDecimal("540.00")
                            }

                            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                                requests.associateWith { it.value }
                        },
                )

            val result =
                service.quote(
                    userId = userId,
                    request =
                        TransferQuoteRequest(
                            originId = originId,
                            targetId = targetId,
                            date = LocalDate.of(2026, 4, 10),
                            originValue = BigDecimal("100.00"),
                        ),
                )

            assertThat(capturedRequest)
                .isEqualTo(
                    ConversionRequest(
                        value = BigDecimal("100.00"),
                        fromCurrency = "USD",
                        toCurrency = "BRL",
                        referenceDate = LocalDate.of(2026, 4, 10),
                    ),
                )
            assertThat(result.targetValue).isEqualByComparingTo(BigDecimal("540.00"))
        }

    @Test
    fun `transferRate should return unit rate for same currency`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val originId = UUID.randomUUID()
            val targetId = UUID.randomUUID()
            val origin = bankAccount(id = originId, userId = userId, currency = "USD")
            val target = bankAccount(id = targetId, userId = userId, currency = "USD")
            val refDate = LocalDate.of(2026, 4, 10)

            val service =
                WalletEntryTransferQuoteServiceImpl(
                    walletItemService = FakeWalletItemService(origin, target),
                    groupService = NoOpGroupService(),
                    exchangeRateService =
                        object : ExchangeRateService {
                            override suspend fun syncLatestQuotes(): Int = 0

                            override suspend fun syncQuotesForDate(
                                date: LocalDate,
                                baseCurrencies: Set<String>?,
                            ): Int = 0

                            override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
                                CursorPage(items = emptyList(), nextCursor = null, hasNext = false)

                            override suspend fun getRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = BigDecimal.ONE

                            override suspend fun resolveRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): ResolvedExchangeRate = error("Same-currency transfer should not resolve stored rate")

                            override suspend fun convert(
                                value: BigDecimal,
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = value

                            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                                requests.associateWith { it.value }
                        },
                )

            val result =
                service.transferRate(
                    userId = userId,
                    request =
                        TransferRateRequest(
                            originId = originId,
                            targetId = targetId,
                            date = refDate,
                        ),
                )

            assertThat(result.rate).isEqualByComparingTo(BigDecimal.ONE)
            assertThat(result.quoteDate).isEqualTo(refDate)
            assertThat(result.baseCurrency).isEqualTo("USD")
            assertThat(result.quoteCurrency).isEqualTo("USD")
        }

    @Test
    fun `transferRate should return resolved rate for differing currencies`() =
        runBlocking {
            val userId = UUID.randomUUID()
            val originId = UUID.randomUUID()
            val targetId = UUID.randomUUID()
            val origin = bankAccount(id = originId, userId = userId, currency = "USD")
            val target = bankAccount(id = targetId, userId = userId, currency = "BRL")

            val service =
                WalletEntryTransferQuoteServiceImpl(
                    walletItemService = FakeWalletItemService(origin, target),
                    groupService = NoOpGroupService(),
                    exchangeRateService =
                        object : ExchangeRateService {
                            override suspend fun syncLatestQuotes(): Int = 0

                            override suspend fun syncQuotesForDate(
                                date: LocalDate,
                                baseCurrencies: Set<String>?,
                            ): Int = 0

                            override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
                                CursorPage(items = emptyList(), nextCursor = null, hasNext = false)

                            override suspend fun getRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = BigDecimal("5.40")

                            override suspend fun resolveRate(
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): ResolvedExchangeRate = ResolvedExchangeRate(rate = BigDecimal("5.40"), quoteDate = LocalDate.of(2026, 4, 9))

                            override suspend fun convert(
                                value: BigDecimal,
                                fromCurrency: String,
                                toCurrency: String,
                                referenceDate: LocalDate,
                            ): BigDecimal = value

                            override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
                                requests.associateWith { it.value }
                        },
                )

            val result =
                service.transferRate(
                    userId = userId,
                    request =
                        TransferRateRequest(
                            originId = originId,
                            targetId = targetId,
                            date = LocalDate.of(2026, 4, 10),
                        ),
                )

            assertThat(result.rate).isEqualByComparingTo(BigDecimal("5.40"))
            assertThat(result.quoteDate).isEqualTo(LocalDate.of(2026, 4, 9))
            assertThat(result.baseCurrency).isEqualTo("USD")
            assertThat(result.quoteCurrency).isEqualTo("BRL")
        }

    private fun bankAccount(
        id: UUID,
        userId: UUID,
        currency: String,
    ): BankAccount =
        BankAccount(
            name = "Test Account",
            enabled = true,
            userId = userId,
            currency = currency,
            balance = BigDecimal.ZERO,
        ).also { it.id = id }

    private class FakeWalletItemService(
        private vararg val items: WalletItem,
    ) : WalletItemService {
        private val itemsById = items.associateBy { it.id!! }

        override suspend fun findAllItems(
            userId: UUID,
            pageable: Pageable,
            onlyBankAccounts: Boolean,
        ): Page<WalletItem> = throw NotImplementedError()

        override suspend fun findOne(id: UUID): WalletItem? = itemsById[id]

        override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletItem> = ids.mapNotNull { id -> itemsById[id] }.asFlow()

        override suspend fun addBalanceById(
            id: UUID,
            balance: BigDecimal,
        ): Long = throw NotImplementedError()
    }

    private class NoOpGroupService : GroupService {
        override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> = throw NotImplementedError()

        override suspend fun findGroup(
            userId: UUID,
            id: UUID,
        ): GroupWithRole? = throw NotImplementedError()

        override suspend fun findGroupWithAssociatedItems(
            userId: UUID,
            id: UUID,
        ): GroupWithRole? = null

        override suspend fun editGroup(
            userId: UUID,
            id: UUID,
            request: EditGroupRequest,
        ): GroupWithRole? = throw NotImplementedError()

        override suspend fun deleteGroup(
            userId: UUID,
            id: UUID,
        ): Boolean = throw NotImplementedError()

        override suspend fun newGroup(
            userId: UUID,
            newGroupRequest: NewGroupRequest,
        ): GroupEntity = throw NotImplementedError()

        override suspend fun findAllMembers(
            userId: UUID,
            id: UUID,
        ): List<GroupUserEntity> = throw NotImplementedError()

        override suspend fun updateMemberRole(
            userId: UUID,
            id: UUID,
            memberId: UUID,
            newRole: UserGroupRole,
        ): Boolean = throw NotImplementedError()

        override suspend fun addNewMember(
            userId: UUID,
            id: UUID,
            role: UserGroupRole,
        ) = throw NotImplementedError()

        override fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity> = emptyList<GroupEntity>().asFlow()
    }
}
