package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.EditCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CreditCardDtoMapperImpl : CreditCardDtoMapper {
    override fun toDto(from: CreditCard): CreditCardDto = CreditCardToDtoMapper.map(from)

    override fun fromDto(from: CreditCardDto): CreditCard = CreditCardFromDtoMapper.map(from)

    override fun fromNewDtoToNewRequest(from: NewCreditCardDto): NewCreditCardRequest = CreditCardFromNewDtoMapper.map(from)

    override fun fromEditDtoToEditRequest(from: EditCreditCardDto): EditCreditCardRequest = CreditCardFromEditDtoMapper.map(from)

    private object CreditCardToDtoMapper : ObjectMappie<CreditCard, CreditCardDto>() {
        override fun map(from: CreditCard) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object CreditCardFromDtoMapper : ObjectMappie<CreditCardDto, CreditCard>() {
        override fun map(from: CreditCardDto) = mapping {}
    }

    private object CreditCardFromNewDtoMapper : ObjectMappie<NewCreditCardDto, NewCreditCardRequest>() {
        override fun map(from: NewCreditCardDto) = mapping {}
    }

    private object CreditCardFromEditDtoMapper : ObjectMappie<EditCreditCardDto, EditCreditCardRequest>() {
        override fun map(from: EditCreditCardDto) = mapping {}
    }
}
