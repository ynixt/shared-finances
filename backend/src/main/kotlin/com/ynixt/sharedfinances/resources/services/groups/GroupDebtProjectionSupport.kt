package com.ynixt.sharedfinances.resources.services.groups

import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import com.ynixt.sharedfinances.domain.models.walletentry.WalletSourceSplit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class GroupDebtProjectionSupport

internal data class ProjectedDebtMovement(
    val payerId: UUID,
    val receiverId: UUID,
    val currency: String,
    val amount: BigDecimal,
)

internal fun deriveProjectedDebtMovementsFromEvent(event: EventListResponse): List<ProjectedDebtMovement> {
    val beneficiaries = event.beneficiaries.takeIf { it.isNotEmpty() } ?: return emptyList()
    val totalMagnitude =
        event.entries
            .fold(BigDecimal.ZERO) { acc, entry -> acc.add(entry.value).asMoney() }
            .abs()
            .asMoney()
    if (totalMagnitude.compareTo(BigDecimal.ZERO) == 0) {
        return emptyList()
    }

    val actualByUser =
        event.entries.fold(linkedMapOf<UUID, BigDecimal>()) { acc, entry ->
            val userId = entry.walletItem.userId
            acc[userId] = acc.getOrDefault(userId, BigDecimal.ZERO).add(entry.value).asMoney()
            acc
        }

    val benefitValues =
        WalletSourceSplit.distributeLegValues(
            type = event.type,
            totalMagnitude = totalMagnitude,
            percents = beneficiaries.map { beneficiary -> beneficiary.benefitPercent },
        )
    val benefitByUser = linkedMapOf<UUID, BigDecimal>()
    beneficiaries.zip(benefitValues).forEach { (beneficiary, value) ->
        benefitByUser[beneficiary.userId] =
            benefitByUser
                .getOrDefault(beneficiary.userId, BigDecimal.ZERO)
                .add(value)
                .asMoney()
    }

    val netByUser =
        (actualByUser.keys + benefitByUser.keys)
            .associateWith { userId ->
                actualByUser
                    .getOrDefault(userId, BigDecimal.ZERO)
                    .subtract(benefitByUser.getOrDefault(userId, BigDecimal.ZERO))
                    .asMoney()
            }

    val debtors =
        netByUser
            .asSequence()
            .filter { (_, value) -> value.compareTo(BigDecimal.ZERO) > 0 }
            .map { (userId, value) -> MutableDebtPosition(userId = userId, remaining = value) }
            .sortedBy { it.userId.toString() }
            .toMutableList()
    val creditors =
        netByUser
            .asSequence()
            .filter { (_, value) -> value.compareTo(BigDecimal.ZERO) < 0 }
            .map { (userId, value) -> MutableDebtPosition(userId = userId, remaining = value) }
            .sortedBy { it.userId.toString() }
            .toMutableList()

    if (debtors.isEmpty() || creditors.isEmpty()) {
        return emptyList()
    }

    val result = mutableListOf<ProjectedDebtMovement>()
    val currency =
        event.entries
            .firstOrNull()
            ?.walletItem
            ?.currency
            ?.uppercase() ?: event.currency.uppercase()
    var debtorIndex = 0
    var creditorIndex = 0

    while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
        val debtor = debtors[debtorIndex]
        val creditor = creditors[creditorIndex]
        val amount = debtor.remaining.min(creditor.remaining.abs()).asMoney()
        if (amount.compareTo(BigDecimal.ZERO) > 0 && debtor.userId != creditor.userId) {
            result.add(
                ProjectedDebtMovement(
                    payerId = debtor.userId,
                    receiverId = creditor.userId,
                    currency = currency,
                    amount = amount,
                ),
            )
        }

        debtor.remaining = debtor.remaining.subtract(amount).asMoney()
        creditor.remaining = creditor.remaining.add(amount).asMoney()
        if (debtor.remaining.compareTo(BigDecimal.ZERO) == 0) {
            debtorIndex++
        }
        if (creditor.remaining.compareTo(BigDecimal.ZERO) == 0) {
            creditorIndex++
        }
    }

    return result
}

private data class MutableDebtPosition(
    val userId: UUID,
    var remaining: BigDecimal,
)

private fun BigDecimal.asMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
