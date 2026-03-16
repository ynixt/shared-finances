package com.ynixt.sharedfinances.resources.services.events.dispatchers

import com.ynixt.sharedfinances.application.web.mapper.BankAccountDtoMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.BankAccountActionEventService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BankAccountActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: BankAccountDtoMapper,
) : BankAccountActionEventService {
    override suspend fun sendInsertedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(bankAccount),
            userId = userId,
            type = ActionEventType.INSERT,
            category = ActionEventCategory.BANK_ACCOUNT,
        )

    override suspend fun sendUpdatedBankAccount(
        userId: UUID,
        bankAccount: BankAccount,
    ) = actionEventService
        .newEvent(
            data = mapper.toDto(bankAccount),
            userId = userId,
            type = ActionEventType.UPDATE,
            category = ActionEventCategory.BANK_ACCOUNT,
        )

    override suspend fun sendDeletedBankAccount(
        userId: UUID,
        id: UUID,
    ) = actionEventService
        .newEvent(
            data = id.toString(),
            userId = userId,
            type = ActionEventType.DELETE,
            category = ActionEventCategory.BANK_ACCOUNT,
        )
}
