package com.ynixt.sharedfinances.resources.services.impl

import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.domain.enums.ActionEventCategory
import com.ynixt.sharedfinances.domain.enums.ActionEventType
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventService
import com.ynixt.sharedfinances.domain.services.actionevents.CreditCardActionEventService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class CreditCardActionEventServiceImpl(
    private val actionEventService: ActionEventService,
    private val mapper: CreditCardDtoMapper,
) : CreditCardActionEventService {
    override fun sendInsertedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(creditCard),
                userId = userId,
                type = ActionEventType.INSERT,
                category = ActionEventCategory.CREDIT_CARD,
            )

    override fun sendUpdatedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = mapper.toDto(creditCard),
                userId = userId,
                type = ActionEventType.UPDATE,
                category = ActionEventCategory.CREDIT_CARD,
            )

    override fun sendDeletedCreditCard(
        userId: UUID,
        id: UUID,
    ): Mono<Long> =
        actionEventService
            .newEvent(
                data = id.toString(),
                userId = userId,
                type = ActionEventType.DELETE,
                category = ActionEventCategory.CREDIT_CARD,
            )
}
