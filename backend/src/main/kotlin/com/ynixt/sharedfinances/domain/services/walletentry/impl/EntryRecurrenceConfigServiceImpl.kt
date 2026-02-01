package com.ynixt.sharedfinances.domain.services.walletentry.impl

import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class EntryRecurrenceConfigServiceImpl(
    private val entryRecurrenceConfigRepository: EntryRecurrenceConfigRepository,
) : EntryRecurrenceConfigService {
    override suspend fun getFutureValuesOfWalletItem(
        walletIdId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BigDecimal =
        merge(
            entryRecurrenceConfigRepository
                .findAllByNextExecutionBetweenAndOriginId(
                    start = startDate,
                    end = endDate,
                    originId = walletIdId,
                ).asFlow(),
            entryRecurrenceConfigRepository
                .findAllByNextExecutionBetweenAndTargetId(
                    start = startDate,
                    end = endDate,
                    targetId = walletIdId,
                ).asFlow(),
        ).toList().sumOf { if (it.originId == walletIdId) it.value else it.value.negate() }
}
