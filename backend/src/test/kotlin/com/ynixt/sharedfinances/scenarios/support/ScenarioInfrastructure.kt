package com.ynixt.sharedfinances.scenarios.support

import com.ynixt.sharedfinances.application.web.dto.GenerateEntryRecurrenceRequestDto
import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.mapper.BankAccountMapper
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.mapper.CreditCardMapper
import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.models.groups.EditGroupRequest
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.queue.producer.GenerateEntryRecurrenceQueueProducer
import com.ynixt.sharedfinances.domain.services.AvatarService
import com.ynixt.sharedfinances.domain.services.DatabaseHelperService
import com.ynixt.sharedfinances.domain.services.actionevents.BankAccountActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.WalletEventActionEventService
import com.ynixt.sharedfinances.domain.services.groups.GroupService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.ArrayDeque
import java.util.UUID

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

internal class NoOpWalletEventActionEventService : WalletEventActionEventService {
    override suspend fun sendInsertedWalletEvent(
        userId: UUID,
        walletEvent: MinimumWalletEventEntity,
    ) {}
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

internal class NoOpGroupService : GroupService {
    override suspend fun findAllGroups(userId: UUID): List<GroupWithRole> = emptyList()

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

    override fun findAllByIdIn(ids: Collection<UUID>): Flow<GroupEntity> = emptyFlow()
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

internal fun nowOffset(): OffsetDateTime = OffsetDateTime.now()
