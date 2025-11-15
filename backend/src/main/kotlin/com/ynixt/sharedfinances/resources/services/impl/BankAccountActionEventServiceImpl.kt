package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.BankAccountActionEventService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class BankAccountActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: BankAccountDtoMapper,
) : BankAccountActionEventService {
    override fun sendInsertedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(bankAccount),
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.BANK_ACCOUNT,
            )

    override fun sendUpdatedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(bankAccount),
                userId = userId,
                type = ActionEventType.UPDATE,
                category = ActionEventCategory.BANK_ACCOUNT,
            )

    override fun sendDeletedBankAccount(
        userId: UUID,
        id: UUID,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = id.toString(),
                userId = userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.BANK_ACCOUNT,
            )
}
