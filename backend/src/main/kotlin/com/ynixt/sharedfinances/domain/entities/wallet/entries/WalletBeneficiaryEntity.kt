package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

abstract class MinimumWalletEventBeneficiaryEntity(
    val walletEventId: UUID,
    val beneficiaryUserId: UUID,
    val benefitPercent: BigDecimal,
) : AuditedEntity() {
    @Transient
    var event: MinimumWalletEventEntity? = null
}

@Table("wallet_event_beneficiary")
class WalletEventBeneficiaryEntity(
    walletEventId: UUID,
    beneficiaryUserId: UUID,
    benefitPercent: BigDecimal,
) : MinimumWalletEventBeneficiaryEntity(
        walletEventId = walletEventId,
        beneficiaryUserId = beneficiaryUserId,
        benefitPercent = benefitPercent,
    )

@Table("recurrence_event_beneficiary")
class RecurrenceEventBeneficiaryEntity(
    walletEventId: UUID,
    beneficiaryUserId: UUID,
    benefitPercent: BigDecimal,
) : MinimumWalletEventBeneficiaryEntity(
        walletEventId = walletEventId,
        beneficiaryUserId = beneficiaryUserId,
        benefitPercent = benefitPercent,
    )
