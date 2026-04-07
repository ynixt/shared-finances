package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.services.BankAccountService
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardService
import com.ynixt.sharedfinances.domain.services.UserService
import com.ynixt.sharedfinances.domain.services.WalletItemService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import com.ynixt.sharedfinances.domain.services.walletentry.recurrence.RecurrenceService
import com.ynixt.sharedfinances.resources.services.BankAccountServiceImpl
import com.ynixt.sharedfinances.resources.services.CreditCardBillServiceImpl
import com.ynixt.sharedfinances.resources.services.CreditCardServiceImpl
import com.ynixt.sharedfinances.resources.services.UserServiceImpl
import com.ynixt.sharedfinances.resources.services.WalletItemServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.WalletEntryCreateServiceImpl
import com.ynixt.sharedfinances.resources.services.walletentry.recurrence.RecurrenceServiceImpl
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryCreditCardBillRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryRecurrenceEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryUserRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEntryRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletEventRepository
import com.ynixt.sharedfinances.scenarios.support.repositories.InMemoryWalletItemRepository
import java.time.LocalDate

internal class ScenarioRuntime(
    initialDate: LocalDate,
) {
    val queueProducer = InMemoryGenerateEntryRecurrenceQueueProducer()
    val clock = MutableScenarioClock(initialDate)

    private val walletItemRepository = InMemoryWalletItemRepository()
    private val walletEventRepository = InMemoryWalletEventRepository()
    private val walletEntryRepository = InMemoryWalletEntryRepository()
    val recurrenceEventRepository: RecurrenceEventRepository = InMemoryRecurrenceEventRepository()
    private val recurrenceEntryRepository = InMemoryRecurrenceEntryRepository()
    private val userRepository = InMemoryUserRepository()
    val creditCardBillRepository = InMemoryCreditCardBillRepository(walletItemRepository)

    private val bankAccountMapper = ScenarioBankAccountMapper()
    private val creditCardMapper = ScenarioCreditCardMapper()
    val creditCardBillMapper: CreditCardBillMapper = ScenarioCreditCardBillMapper()
    private val walletItemMapper = ScenarioWalletItemMapper(bankAccountMapper, creditCardMapper)

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

    val walletEntryCreateService: WalletEntryCreateService =
        WalletEntryCreateServiceImpl(
            walletEventRepository = walletEventRepository,
            walletEntryRepository = walletEntryRepository,
            groupService = NoOpGroupService(),
            walletItemService = walletItemService,
            creditCardBillService = creditCardBillService,
            recurrenceService = recurrenceService,
            recurrenceEventRepository = recurrenceEventRepository,
            recurrenceEntryRepository = recurrenceEntryRepository,
            walletEventActionEventService = NoOpWalletEventActionEventService(),
            walletItemMapper = walletItemMapper,
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

    val userService: UserService =
        UserServiceImpl(
            repository = userRepository,
            passwordEncoder = ScenarioPasswordEncoder(),
            databaseHelperService = NoOpDatabaseHelperService(),
            avatarService = NoOpAvatarService(),
        )
}
