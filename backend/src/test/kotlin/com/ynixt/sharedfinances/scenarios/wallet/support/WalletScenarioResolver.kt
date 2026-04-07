package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.MinimumWalletEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.models.WalletItem
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import kotlinx.coroutines.reactor.awaitSingleOrNull
import java.time.LocalDate
import java.util.UUID

internal class WalletScenarioResolver(
    private val runtime: ScenarioRuntime,
    private val context: WalletScenarioContext,
    private val createDefaultUser: suspend () -> Unit,
) {
    data class ResolvedExpenseOrigin(
        val originId: UUID,
        val billDate: LocalDate?,
    )

    suspend fun ensureUser(): UUID {
        val current = context.currentUserId
        if (current != null) {
            return current
        }
        createDefaultUser()
        return requireNotNull(context.currentUserId)
    }

    suspend fun getCreditCard(id: UUID): CreditCard {
        val model = requireNotNull(runtime.walletItemService.findOne(id)) { "Wallet item $id was not found" }
        return model as? CreditCard ?: error("Wallet item $id is not a credit card")
    }

    suspend fun resolveCreditCard(creditCardId: UUID? = null): CreditCard = getCreditCard(creditCardId ?: requireCurrentCreditCardId())

    fun resolveOriginId(originId: UUID? = null): UUID =
        originId ?: context.currentCreditCardId ?: context.currentBankAccountId ?: error("No origin account available")

    suspend fun resolveExpenseOrigin(
        originId: UUID? = null,
        date: LocalDate,
        billDate: LocalDate? = null,
    ): ResolvedExpenseOrigin {
        val resolvedOriginId = resolveOriginId(originId)
        val origin =
            requireNotNull(runtime.walletItemService.findOne(resolvedOriginId)) {
                "Wallet item $resolvedOriginId was not found"
            }
        val resolvedBillDate =
            if (origin is CreditCard) {
                billDate ?: origin.getBestBill(date)
            } else {
                null
            }

        return ResolvedExpenseOrigin(
            originId = resolvedOriginId,
            billDate = resolvedBillDate,
        )
    }

    suspend fun resolveBankAccountItem(bankAccountId: UUID? = null): WalletItem {
        val accountId = bankAccountId ?: requireCurrentBankAccountId()
        return requireNotNull(runtime.walletItemService.findOne(accountId)) {
            "Wallet item $accountId was not found"
        }
    }

    suspend fun getNextRecurrenceExecutionDate(recurrenceConfigId: UUID? = null): LocalDate {
        val recurrence = requireRecurrenceEntity(recurrenceConfigId)
        return requireNotNull(recurrence.nextExecution) {
            "Recurrence config ${recurrence.id} has no next execution date"
        }
    }

    private suspend fun findBillEntityByCardAndDate(
        creditCardId: UUID,
        billDate: LocalDate,
    ): CreditCardBillEntity? =
        runtime.creditCardBillRepository
            .findOneByCreditCardIdAndBillDate(
                creditCardId = creditCardId,
                billDate = billDate,
            ).awaitSingleOrNull()

    suspend fun requireBillEntity(
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ): CreditCardBillEntity {
        val resolvedCreditCardId = creditCardId ?: requireCurrentCreditCardId()
        return requireNotNull(findBillEntityByCardAndDate(resolvedCreditCardId, billDate)) {
            "Bill for card $resolvedCreditCardId on $billDate was not found"
        }
    }

    suspend fun findBillEntity(
        billDate: LocalDate,
        creditCardId: UUID? = null,
    ): CreditCardBillEntity? {
        val resolvedCreditCardId = creditCardId ?: requireCurrentCreditCardId()
        return findBillEntityByCardAndDate(
            creditCardId = resolvedCreditCardId,
            billDate = billDate,
        )
    }

    suspend fun resolveBill(
        billId: UUID? = null,
        billDate: LocalDate? = null,
        creditCardId: UUID? = null,
    ): CreditCardBill =
        when {
            billId != null ->
                requireNotNull(runtime.creditCardBillService.findById(billId)) {
                    "Bill $billId was not found"
                }

            billDate != null -> runtime.creditCardBillMapper.toModel(requireBillEntity(billDate = billDate, creditCardId = creditCardId))

            context.lastBillId != null ->
                requireNotNull(runtime.creditCardBillService.findById(context.lastBillId!!)) {
                    "Last tracked bill ${context.lastBillId} was not found"
                }

            else -> error("Provide billId, billDate, or create a bill before asserting")
        }

    suspend fun requireRecurrenceEntity(recurrenceConfigId: UUID? = null): RecurrenceEventEntity {
        val id = recurrenceConfigId ?: requireNotNull(context.lastRecurrenceConfigId) { "No tracked recurrence config found" }
        return runtime.recurrenceEventRepository.findById(id).awaitSingleOrNull() ?: error("Recurrence config $id not found")
    }

    fun requireCurrentCreditCardId(): UUID =
        requireNotNull(context.currentCreditCardId) {
            "No credit card in scenario. Call creditCard() / givenCreditCard() first."
        }

    fun requireCurrentBankAccountId(): UUID =
        requireNotNull(context.currentBankAccountId) {
            "No bank account in scenario. Call bankAccount() / givenBankAccount() first."
        }

    fun extractRecurrenceId(created: MinimumWalletEventEntity?): UUID? =
        when (created) {
            null -> null
            is WalletEventEntity -> created.recurrenceEvent?.id ?: created.recurrenceEventId
            is RecurrenceEventEntity -> created.id
            else -> null
        }
}
