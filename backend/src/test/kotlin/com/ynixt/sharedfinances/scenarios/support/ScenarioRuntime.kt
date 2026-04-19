package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.LegalDocumentProperties
import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByGoalRow
import com.ynixt.sharedfinances.domain.repositories.GoalCommittedByWalletRow
import com.ynixt.sharedfinances.domain.repositories.GoalCurrencyCommittedRow
import com.ynixt.sharedfinances.domain.repositories.GoalLedgerCommittedSummaryRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceSeriesRepository
import com.ynixt.sharedfinances.domain.services.BankAccountService
import com.ynixt.sharedfinances.domain.services.CreditCardBillPaymentService
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.dashboard.OverviewDashboardService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import com.ynixt.sharedfinances.domain.services.walletentry.ScheduledExecutionManagerService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryEditService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryRemovalService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEventListService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceOccurrenceSimulationService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceSimulationService
import com.ynixt.sharedfinances.resources.services.BankAccountServiceImpl
import com.ynixt.sharedfinances.resources.services.CreditCardBillPaymentServiceImpl
import com.ynixt.sharedfinances.resources.services.CreditCardBillServiceImpl
import com.ynixt.sharedfinances.resources.services.CreditCardServiceImpl
import com.ynixt.sharedfinances.resources.services.UserServiceImpl
import com.ynixt.sharedfinances.resources.services.WalletItemServiceImpl
import com.ynixt.sharedfinances.resources.services.dashboard.OverviewDashboardServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.ScheduledExecutionManagerServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.WalletEntryCreateServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.WalletEntryEditServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.WalletEntryRemovalServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.WalletEventListServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.recurrence.RecurrenceOccurrenceSimulationServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.recurrence.RecurrenceServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.recurrence.RecurrenceSimulationServiceImpl
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryCreditCardBillRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceSeriesRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

internal fun identityExchangeRateService(): ExchangeRateService =
    object : ExchangeRateService {
        override suspend fun syncLatestQuotes(): Int = 0

        override suspend fun syncQuotesForDate(
            date: LocalDate,
            baseCurrencies: Set<String>?,
        ): Int = 0

        override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> =
            CursorPage(
                items = emptyList(),
                nextCursor = null,
                hasNext = false,
            )

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
        ): BigDecimal = value

        override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> =
            requests.associateWith { it.value }
    }

internal class ScenarioRuntime(
    initialDate: LocalDate,
    private val groupService: GroupService = NoOpGroupService(),
    private val exchangeRateService: ExchangeRateService = identityExchangeRateService(),
) {
    val queueProducer = InMemoryGenerateEntryRecurrenceQueueProducer()
    val clock = MutableScenarioClock(initialDate)

    private val walletItemRepository = InMemoryWalletItemRepository()
    private val walletEventRepository = InMemoryWalletEventRepository(walletItemRepository)
    private val walletEntryRepository = InMemoryWalletEntryRepository(walletEventRepository, walletItemRepository)
    private val recurrenceEventRepositoryRaw = InMemoryRecurrenceEventRepository(walletItemRepository)
    private val recurrenceEntryRepository = InMemoryRecurrenceEntryRepository()
    private val recurrenceSeriesRepositoryRaw =
        InMemoryRecurrenceSeriesRepository { seriesId ->
            val recurrenceEventIds = recurrenceEventRepositoryRaw.findIdsBySeriesId(seriesId)
            if (recurrenceEventIds.isEmpty()) {
                return@InMemoryRecurrenceSeriesRepository
            }

            recurrenceEntryRepository.deleteAllByWalletEventIds(recurrenceEventIds)
            val walletEventIds = walletEventRepository.findIdsByRecurrenceEventIds(recurrenceEventIds)
            walletEntryRepository.deleteAllByWalletEventIds(walletEventIds)
            walletEventRepository.deleteAllByRecurrenceEventIds(recurrenceEventIds)
            recurrenceEventRepositoryRaw.deleteAllBySeriesId(seriesId)
        }
    val recurrenceEventRepository: RecurrenceEventRepository = recurrenceEventRepositoryRaw
    val recurrenceSeriesRepository: RecurrenceSeriesRepository = recurrenceSeriesRepositoryRaw
    private val userRepository = InMemoryUserRepository()
    val creditCardBillRepository = InMemoryCreditCardBillRepository(walletItemRepository)
    private val walletEventBeneficiaryRepository = inMemoryWalletEventBeneficiaryRepository()
    private val recurrenceEventBeneficiaryRepository = inMemoryRecurrenceEventBeneficiaryRepository()

    private val bankAccountMapper = ScenarioBankAccountMapper()
    private val creditCardMapper = ScenarioCreditCardMapper()
    val creditCardBillMapper: CreditCardBillMapper = ScenarioCreditCardBillMapper()
    private val walletItemMapper = ScenarioWalletItemMapper(bankAccountMapper, creditCardMapper)
    private val genericCategoryService: GenericCategoryService = NoOpGenericCategoryService()
    val walletEventActionEventService = RecordingWalletEventActionEventService()

    val walletItemService: WalletItemService = WalletItemServiceImpl(walletItemRepository, walletItemMapper)

    val recurrenceService: RecurrenceService =
        RecurrenceServiceImpl(
            repository = recurrenceEventRepository,
            queueProducer = queueProducer,
            clock = clock,
        )

    val creditCardService: CreditCardService =
        CreditCardServiceImpl(
            walletItemRepository = walletItemRepository,
            walletEventRepository = walletEventRepository,
            recurrenceEventRepository = recurrenceEventRepository,
            creditCardActionEventService = NoOpCreditCardActionEventService(),
            creditCardMapper = creditCardMapper,
        )

    val creditCardBillService: CreditCardBillService =
        CreditCardBillServiceImpl(
            creditCardBillRepository = creditCardBillRepository,
            creditCardBillMapper = creditCardBillMapper,
            creditCardService = creditCardService,
        )

    val userService: UserService =
        UserServiceImpl(
            repository = userRepository,
            passwordEncoder = ScenarioPasswordEncoder(),
            databaseHelperService = NoOpDatabaseHelperService(),
            avatarService = NoOpAvatarService(),
            legalDocumentProperties = LegalDocumentProperties(),
            authProperties = AuthProperties(),
            clock = clock,
            accountDeletionService = NoOpAccountDeletionService,
        )

    val recurrenceOccurrenceSimulationService: RecurrenceOccurrenceSimulationService =
        RecurrenceOccurrenceSimulationServiceImpl(
            creditCardBillService = creditCardBillService,
        )

    val recurrenceSimulationService: RecurrenceSimulationService =
        RecurrenceSimulationServiceImpl(
            genericCategoryService = genericCategoryService,
            groupService = groupService,
            userService = userService,
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            walletItemMapper = walletItemMapper,
            recurrenceService = recurrenceService,
            recurrenceOccurrenceSimulationService = recurrenceOccurrenceSimulationService,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            clock = clock,
        )

    val walletEventListService: WalletEventListService =
        WalletEventListServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            creditCardBillRepository = creditCardBillRepository,
            walletItemMapper = walletItemMapper,
            walletItemService = walletItemService,
            genericCategoryService = genericCategoryService,
            groupService = groupService,
            groupPermissionService = ScenarioGroupPermissionService(groupService),
            userService = userService,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            recurrenceSimulationService = recurrenceSimulationService,
            recurrenceService = recurrenceService,
            walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
            recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
        )

    val walletEntryCreateService: WalletEntryCreateService =
        WalletEntryCreateServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            groupService = groupService,
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            recurrenceService = recurrenceService,
            recurrenceEventRepository = recurrenceEventRepository,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            groupDebtService = NoOpGroupDebtService,
            walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
            recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
            walletEventActionEventService = walletEventActionEventService,
            walletItemMapper = walletItemMapper,
            exchangeRateService = exchangeRateService,
            clock = clock,
        )

    val messageSource: MessageSource =
        object : MessageSource {
            override fun getMessage(
                code: String,
                args: Array<out Any>?,
                defaultMessage: String?,
                locale: Locale?,
            ): String = code

            override fun getMessage(
                code: String,
                args: Array<out Any>?,
                locale: Locale?,
            ): String = code

            override fun getMessage(
                resolvable: MessageSourceResolvable,
                locale: Locale?,
            ): String =
                resolvable.codes?.firstOrNull()
                    ?: resolvable.defaultMessage
                    ?: throw NoSuchMessageException("")
        }

    val walletEntryEditService: WalletEntryEditService =
        WalletEntryEditServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            groupService = groupService,
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            recurrenceService = recurrenceService,
            recurrenceEventRepository = recurrenceEventRepository,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            groupDebtService = NoOpGroupDebtService,
            walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
            recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
            walletEventActionEventService = walletEventActionEventService,
            walletItemMapper = walletItemMapper,
            clock = clock,
        )

    val walletEntryRemovalService: WalletEntryRemovalService =
        WalletEntryRemovalServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            groupService = groupService,
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            recurrenceService = recurrenceService,
            recurrenceEventRepository = recurrenceEventRepository,
            recurrenceSeriesRepository = recurrenceSeriesRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            groupDebtService = NoOpGroupDebtService,
            walletEventBeneficiaryRepository = walletEventBeneficiaryRepository,
            recurrenceEventBeneficiaryRepository = recurrenceEventBeneficiaryRepository,
            walletEventActionEventService = walletEventActionEventService,
            walletItemMapper = walletItemMapper,
            clock = clock,
        )

    val scheduledExecutionManagerService: ScheduledExecutionManagerService =
        ScheduledExecutionManagerServiceImpl(
            recurrenceSimulationService = recurrenceSimulationService,
            walletEventRepository = walletEventRepository,
            walletEventListService = walletEventListService,
            groupPermissionService = ScenarioGroupPermissionService(groupService),
            clock = clock,
        )

    val bankAccountService: BankAccountService =
        BankAccountServiceImpl(
            walletItemRepository = walletItemRepository,
            walletEventRepository = walletEventRepository,
            recurrenceEventRepository = recurrenceEventRepository,
            bankAccountActionEventService = NoOpBankAccountActionEventService(),
            bankAccountMapper = bankAccountMapper,
            walletEntryCreateService = walletEntryCreateService,
            clock = clock,
        )

    val creditCardBillPaymentService: CreditCardBillPaymentService =
        CreditCardBillPaymentServiceImpl(
            creditCardBillService = creditCardBillService,
            bankAccountService = bankAccountService,
            walletEntryCreateService = walletEntryCreateService,
            messageSource = messageSource,
        )

    val overviewDashboardService: OverviewDashboardService =
        run {
            val balanceService =
                com.ynixt.sharedfinances.resources.services.dashboard
                    .OverviewDashboardBalanceServiceImpl()

            OverviewDashboardServiceImpl(
                dataService =
                    com.ynixt.sharedfinances.resources.services.dashboard.OverviewDashboardDataServiceImpl(
                        walletItemRepository = walletItemRepository,
                        walletItemMapper = walletItemMapper,
                        walletEntryRepository = walletEntryRepository,
                        recurrenceSimulationService = recurrenceSimulationService,
                        creditCardBillService = creditCardBillService,
                        groupService = groupService,
                        groupDebtService = NoOpGroupDebtService,
                        clock = clock,
                    ),
                balanceService = balanceService,
                contributionService =
                    com.ynixt.sharedfinances.resources.services.dashboard
                        .OverviewDashboardContributionServiceImpl(balanceService),
                goalService =
                    com.ynixt.sharedfinances.resources.services.dashboard.OverviewDashboardGoalServiceImpl(
                        NoOpGoalLedgerCommittedSummaryRepository,
                    ),
                assemblyService =
                    com.ynixt.sharedfinances.resources.services.dashboard
                        .OverviewDashboardAssemblyServiceImpl(exchangeRateService),
                cardService =
                    com.ynixt.sharedfinances.resources.services.dashboard
                        .OverviewDashboardCardServiceImpl(),
                chartService =
                    com.ynixt.sharedfinances.resources.services.dashboard
                        .OverviewDashboardChartServiceImpl(),
                clock = clock,
            )
        }
}

internal object NoOpGoalLedgerCommittedSummaryRepository : GoalLedgerCommittedSummaryRepository {
    override fun summarizeCommittedByUserGoals(userId: java.util.UUID): Flux<GoalCommittedByWalletRow> = Flux.empty()

    override fun summarizeCommittedByUserGoalsDetailed(userId: java.util.UUID): Flux<GoalCommittedByGoalRow> = Flux.empty()

    override fun summarizeCommittedByGoal(goalId: java.util.UUID): Flux<GoalCurrencyCommittedRow> = Flux.empty()
}
