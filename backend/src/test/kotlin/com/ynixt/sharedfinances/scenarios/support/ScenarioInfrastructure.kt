package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.exchangerate.ExchangeRateQuoteEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletCategoryConceptEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryCategoryEntity
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletCategoryConceptCode
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.exceptions.http.ExchangeRateUnavailableException
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.CursorPage
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.exchangerate.ExchangeRateQuoteListRequest
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.services.AccountDeletionService
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.BankAccountActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.UserActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.categories.CategoryConceptService
import com.ynixt.sharedfinances.domain.services.categories.GenericCategoryService
import com.ynixt.sharedfinances.domain.services.exchangerate.ConversionRequest
import com.ynixt.sharedfinances.domain.services.exchangerate.ExchangeRateService
import com.ynixt.sharedfinances.domain.services.exchangerate.ResolvedExchangeRate
import com.ynixt.sharedfinances.domain.services.groups.GroupPermissionService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.absoluteValue

internal class MutableScenarioClock(
    private var date: LocalDate,
    private val zoneId: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableScenarioClock(date, zone)

    override fun instant(): Instant = date.atStartOfDay(zoneId).toInstant()

    fun setDate(newDate: LocalDate) {
        date = newDate
    }

    fun today(): LocalDate = date
}

internal class ScenarioPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String = rawPassword?.toString() ?: ""

    override fun matches(
        rawPassword: CharSequence?,
        encodedPassword: String?,
    ): Boolean = rawPassword?.toString() == encodedPassword
}

internal class NoOpDatabaseHelperService : DatabaseHelperService {
    override fun isUniqueViolation(
        t: Throwable,
        indexName: String,
    ): Boolean = false
}

internal class NoOpAvatarService : AvatarService {
    override suspend fun getPhotoFromGravatar(
        email: String,
        userId: UUID,
    ): String? = null

    override suspend fun deletePhoto(userId: UUID): Boolean = true

    override suspend fun upload(
        userId: UUID,
        bytes: ByteArray,
        contentType: String,
    ): String = "scenario://avatar/$userId"

    override suspend fun upload(
        userId: UUID,
        file: FilePart,
    ): String = "scenario://avatar/$userId"
}

internal object NoOpAccountDeletionService : AccountDeletionService {
    override suspend fun deleteAccountForUser(userId: UUID) {}
}

internal class NoOpWalletEventActionEventService : WalletEventActionEventService {
    override suspend fun sendInsertedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {}

    override suspend fun sendUpdatedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {}

    override suspend fun sendDeletedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {}
}

internal class RecordingWalletEventActionEventService : WalletEventActionEventService {
    data class PublishedEvent(
        val type: ActionEventType,
        val userId: UUID,
        val walletEvent: MinimumWalletEventEntity,
    )

    private val events = mutableListOf<PublishedEvent>()

    val publishedEvents: List<PublishedEvent>
        get() = events.toList()

    override suspend fun sendInsertedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {
        events.add(PublishedEvent(type = ActionEventType.INSERT, userId = userId, walletEvent = walletEvent))
    }

    override suspend fun sendUpdatedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {
        events.add(PublishedEvent(type = ActionEventType.UPDATE, userId = userId, walletEvent = walletEvent))
    }

    override suspend fun sendDeletedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {
        events.add(PublishedEvent(type = ActionEventType.DELETE, userId = userId, walletEvent = walletEvent))
    }

    fun clear() {
        events.clear()
    }
}

internal class NoOpCreditCardActionEventService : CreditCardActionEventService {
    override suspend fun sendInsertedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ) {}

    override suspend fun sendUpdatedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ) {}

    override suspend fun sendDeletedCreditCard(
        userId: UUID,
        id: UUID,
    ) {}
}

internal class NoOpBankAccountActionEventService : BankAccountActionEventService {
    override suspend fun sendInsertedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ) {}

    override suspend fun sendUpdatedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ) {}

    override suspend fun sendDeletedBankAccount(
        userId: UUID,
        id: UUID,
    ) {}
}

internal class NoOpUserActionEventService : UserActionEventService {
    override suspend fun sendUpdatedUser(
        userId: UUID,
        user: UserEntity,
    ) {}
}

internal class NoOpGroupService : GroupService {
    override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> = emptyList()

    override suspend fun searchGroups(
        userId: UUID,
        pageable: Pageable,
        query: String?,
    ): Page<GroupWithRole> = PageImpl(emptyList(), pageable, 0)

    override suspend fun findGroup(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? = null

    override suspend fun findGroupWithAssociatedItems(
        userId: UUID,
        id: UUID,
    ): GroupWithRole? = null

    override suspend fun editGroup(
        userId: UUID,
        id: UUID,
        request: EditGroupRequest,
    ): GroupWithRole? = null

    override suspend fun deleteGroup(
        userId: UUID,
        id: UUID,
    ): Boolean = false

    override suspend fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): GroupEntity = error("Group operations are not part of this scenario DSL")

    override suspend fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): List<GroupUserEntity> = emptyList()

    override suspend fun updateMemberRole(
        userId: UUID,
        id: UUID,
        memberId: UUID,
        newRole: UserGroupRole,
    ): Boolean = false

    override suspend fun addNewMember(
        userId: UUID,
        id: UUID,
        role: UserGroupRole,
    ) {}

    override suspend fun updateOwnPlanningSimulatorOptIn(
        userId: UUID,
        id: UUID,
        allowPlanningSimulator: Boolean,
    ): Boolean = false

    override fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity> = emptyFlow()
}

internal class NoOpGenericCategoryService : GenericCategoryService {
    override suspend fun findById(id: UUID): WalletEntryCategoryEntity? = null

    override fun findAllByIdIn(ids: Collection<UUID>): Flow<WalletEntryCategoryEntity> = emptyFlow()

    override suspend fun findAllByGroupIdAndConceptId(
        groupId: UUID,
        conceptId: UUID,
    ): List<WalletEntryCategoryEntity> = emptyList()
}

internal object NoOpCategoryConceptService : CategoryConceptService {
    override suspend fun findById(id: UUID): WalletCategoryConceptEntity? = null

    override suspend fun findRequiredByCode(code: WalletCategoryConceptCode): WalletCategoryConceptEntity = error("not used")

    override suspend fun listAvailableForUser(userId: UUID): List<WalletCategoryConceptEntity> = emptyList()

    override suspend fun resolveForMutation(
        conceptId: UUID?,
        customConceptName: String?,
    ): WalletCategoryConceptEntity = error("not used")

    override suspend fun cleanupOrphanedCustomConcept(conceptId: UUID): Boolean = false
}

internal class ScenarioGroupPermissionService(
    private val groupService: GroupService,
) : GroupPermissionService {
    override suspend fun hasPermission(
        userId: UUID,
        groupId: UUID,
        permission: GroupPermissions?,
    ): Boolean {
        val group = groupService.findGroup(userId, groupId) ?: return false
        return permission == null || group.permissions.contains(permission)
    }

    override fun getAllPermissionsForRole(role: UserGroupRole): Set<GroupPermissions> =
        when (role) {
            UserGroupRole.ADMIN -> GroupPermissions.entries.toSet()
            UserGroupRole.EDITOR ->
                setOf(
                    GroupPermissions.SEND_ENTRIES,
                    GroupPermissions.ADD_BANK_ACCOUNT,
                    GroupPermissions.MANAGE_GOALS,
                    GroupPermissions.NEW_SIMULATION,
                    GroupPermissions.DELETE_SIMULATIONS,
                )

            else -> emptySet()
        }
}

internal class InMemoryGenerateEntryRecurrenceQueueProducer : GenerateEntryRecurrenceQueueProducer {
    private val queue = ArrayDeque<GenerateEntryRecurrenceRequestDto>()

    override fun send(request: GenerateEntryRecurrenceRequestDto) {
        queue.addLast(request)
    }

    fun poll(): GenerateEntryRecurrenceRequestDto? =
        if (queue.isEmpty()) {
            null
        } else {
            queue.removeFirst()
        }
}

internal class ScenarioBankAccountMapper : BankAccountMapper {
    override fun toEntity(from: BankAccount): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = from.name,
            enabled = from.enabled,
            userId = from.userId,
            currency = from.currency,
            balance = from.balance,
            showOnDashboard = from.showOnDashboard,
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
        ).also {
            it.id = from.id
        }

    override fun toModel(from: WalletItemEntity): BankAccount =
        BankAccount(
            name = from.name,
            enabled = from.enabled,
            userId = from.userId,
            currency = from.currency,
            balance = from.balance,
            showOnDashboard = from.showOnDashboard,
        ).also {
            it.id = from.id
            it.createdAt = from.createdAt
            it.updatedAt = from.updatedAt
        }
}

internal class ScenarioCreditCardMapper : CreditCardMapper {
    override fun toEntity(from: CreditCard): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.CREDIT_CARD,
            name = from.name,
            enabled = from.enabled,
            userId = from.userId,
            currency = from.currency,
            balance = from.balance,
            totalLimit = from.totalLimit,
            dueDay = from.dueDay,
            daysBetweenDueAndClosing = from.daysBetweenDueAndClosing,
            dueOnNextBusinessDay = from.dueOnNextBusinessDay,
            showOnDashboard = from.showOnDashboard,
        ).also {
            it.id = from.id
        }

    override fun toModel(from: WalletItemEntity): CreditCard =
        CreditCard(
            name = from.name,
            enabled = from.enabled,
            userId = from.userId,
            currency = from.currency,
            totalLimit = requireNotNull(from.totalLimit),
            balance = from.balance,
            dueDay = requireNotNull(from.dueDay),
            daysBetweenDueAndClosing = requireNotNull(from.daysBetweenDueAndClosing),
            dueOnNextBusinessDay = requireNotNull(from.dueOnNextBusinessDay),
            showOnDashboard = from.showOnDashboard,
        ).also {
            it.id = from.id
            it.createdAt = from.createdAt
            it.updatedAt = from.updatedAt
        }
}

internal class ScenarioWalletItemMapper(
    private val bankAccountMapper: BankAccountMapper,
    private val creditCardMapper: CreditCardMapper,
) : WalletItemMapper {
    override fun toModel(from: WalletItemEntity): WalletItem =
        when (from.type) {
            WalletItemType.BANK_ACCOUNT -> bankAccountMapper.toModel(from)
            WalletItemType.CREDIT_CARD -> creditCardMapper.toModel(from)
        }

    override fun fromModel(from: WalletItem): WalletItemEntity =
        when (from) {
            is BankAccount -> bankAccountMapper.toEntity(from)
            is CreditCard -> creditCardMapper.toEntity(from)
            else -> error("Unsupported wallet item model ${from::class}")
        }
}

internal class ScenarioCreditCardBillMapper : CreditCardBillMapper {
    override fun toModel(entity: CreditCardBillEntity): CreditCardBill =
        CreditCardBill(
            id = entity.id,
            creditCardId = entity.creditCardId,
            billDate = entity.billDate,
            dueDate = entity.dueDate,
            closingDate = entity.closingDate,
            paid = entity.paid,
            value = entity.value,
        )
}

internal class ScenarioStoredExchangeRateService : ExchangeRateService {
    private val quotes = mutableListOf<ExchangeRateQuoteEntity>()

    fun storeQuote(
        baseCurrency: String,
        quoteCurrency: String,
        quoteDate: LocalDate,
        rate: BigDecimal,
        source: String = "scenario-test",
        quotedAt: OffsetDateTime = quoteDate.atStartOfDay().atOffset(ZoneOffset.UTC),
        fetchedAt: OffsetDateTime = quotedAt,
    ) {
        val entity =
            ExchangeRateQuoteEntity(
                source = source,
                baseCurrency = baseCurrency.uppercase(),
                quoteCurrency = quoteCurrency.uppercase(),
                quoteDate = quoteDate,
                rate = rate,
                quotedAt = quotedAt,
                fetchedAt = fetchedAt,
            ).also { quote ->
                quote.id = UUID.randomUUID()
            }
        quotes.add(entity)
    }

    override suspend fun syncLatestQuotes(): Int = 0

    override suspend fun syncQuotesForDate(
        date: LocalDate,
        baseCurrencies: Set<String>?,
    ): Int = 0

    override suspend fun listQuotes(request: ExchangeRateQuoteListRequest): CursorPage<ExchangeRateQuoteEntity> {
        val filtered =
            quotes
                .asSequence()
                .filter { quote ->
                    request.baseCurrency == null || quote.baseCurrency == request.baseCurrency.uppercase()
                }.filter { quote ->
                    request.quoteCurrency == null || quote.quoteCurrency == request.quoteCurrency.uppercase()
                }.filter { quote ->
                    request.quoteDateFrom == null || !quote.quoteDate.isBefore(request.quoteDateFrom)
                }.filter { quote ->
                    request.quoteDateTo == null || !quote.quoteDate.isAfter(request.quoteDateTo)
                }.sortedWith(
                    compareByDescending<ExchangeRateQuoteEntity> { it.quoteDate }
                        .thenByDescending { it.quotedAt }
                        .thenByDescending { it.id?.toString().orEmpty() },
                ).toList()

        return CursorPage(
            items = filtered,
            nextCursor = null,
            hasNext = false,
        )
    }

    override suspend fun getRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal = resolveRate(fromCurrency, toCurrency, referenceDate).rate

    override suspend fun resolveRate(
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): ResolvedExchangeRate {
        val normalizedFrom = fromCurrency.uppercase()
        val normalizedTo = toCurrency.uppercase()

        if (normalizedFrom == normalizedTo) {
            return ResolvedExchangeRate(rate = BigDecimal.ONE, quoteDate = referenceDate)
        }

        val selected =
            selectQuote(normalizedFrom, normalizedTo, referenceDate)
                ?: throw ExchangeRateUnavailableException(normalizedFrom, normalizedTo, referenceDate)

        return ResolvedExchangeRate(
            rate = selected.rate,
            quoteDate = selected.quoteDate,
        )
    }

    override suspend fun convert(
        value: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        referenceDate: LocalDate,
    ): BigDecimal =
        value
            .multiply(getRate(fromCurrency, toCurrency, referenceDate))
            .setScale(2, RoundingMode.HALF_UP)

    override suspend fun convertBatch(requests: Collection<ConversionRequest>): Map<ConversionRequest, BigDecimal> {
        val result = linkedMapOf<ConversionRequest, BigDecimal>()
        requests.forEach { request ->
            val rate =
                getRate(
                    fromCurrency = request.fromCurrency,
                    toCurrency = request.toCurrency,
                    referenceDate = request.referenceDate,
                )
            result[request] =
                request.value
                    .multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP)
        }
        return result
    }

    private fun selectQuote(
        baseCurrency: String,
        quoteCurrency: String,
        referenceDate: LocalDate,
    ): ExchangeRateQuoteEntity? {
        val pairQuotes =
            quotes
                .asSequence()
                .filter { quote ->
                    quote.baseCurrency == baseCurrency && quote.quoteCurrency == quoteCurrency
                }.sortedWith(
                    compareBy<ExchangeRateQuoteEntity> { it.quoteDate }
                        .thenBy { it.quotedAt }
                        .thenBy { it.id?.toString().orEmpty() },
                ).toList()

        if (pairQuotes.isEmpty()) {
            return null
        }

        val before = pairQuotes.lastOrNull { !it.quoteDate.isAfter(referenceDate) }
        val after = pairQuotes.firstOrNull { !it.quoteDate.isBefore(referenceDate) }

        return when {
            before == null && after == null -> null
            before == null -> after
            after == null -> before
            else -> chooseClosest(referenceDate = referenceDate, before = before, after = after)
        }
    }

    private fun chooseClosest(
        referenceDate: LocalDate,
        before: ExchangeRateQuoteEntity,
        after: ExchangeRateQuoteEntity,
    ): ExchangeRateQuoteEntity {
        val beforeDistance = ChronoUnit.DAYS.between(before.quoteDate, referenceDate).absoluteValue
        val afterDistance = ChronoUnit.DAYS.between(referenceDate, after.quoteDate).absoluteValue

        return if (beforeDistance <= afterDistance) before else after
    }
}

internal fun nowOffset(): OffsetDateTime = OffsetDateTime.now()
