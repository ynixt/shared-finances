package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.EditCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.application.web.mapper.CreditCardDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class CreditCardDtoMapperImpl(
    userDtoMapper: UserDtoMapper,
) : CreditCardDtoMapper {
    private val associateMapper = CreditCardToAssociateDtoMapper(userDtoMapper)

    override fun toDto(from: CreditCard): CreditCardDto = CreditCardToDtoMapper.map(from)

    override fun fromDto(from: CreditCardDto): CreditCard = CreditCardFromDtoMapper.map(from)

    override fun fromNewDtoToNewRequest(from: NewCreditCardDto): NewCreditCardRequest = CreditCardFromNewDtoMapper.map(from)

    override fun fromEditDtoToEditRequest(from: EditCreditCardDto): EditCreditCardRequest = CreditCardFromEditDtoMapper.map(from)

    override fun toAssociateDto(from: CreditCard): CreditCardForGroupAssociateDto = associateMapper.map(from)

    private object CreditCardToDtoMapper : ObjectMappie<CreditCard, CreditCardDto>() {
        override fun map(from: CreditCard) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private class CreditCardToAssociateDtoMapper(
        private val userDtoMapper: UserDtoMapper,
    ) : ObjectMappie<CreditCard, CreditCardForGroupAssociateDto>() {
        override fun map(from: CreditCard) =
            mapping {
                to::id fromPropertyNotNull from::id
                to::user fromPropertyNotNull from::user transform { userDtoMapper.tSimpleDto(it) }
            }
    }

    private object CreditCardFromDtoMapper : ObjectMappie<CreditCardDto, CreditCard>() {
        override fun map(from: CreditCardDto) = mapping {}
    }

    private object CreditCardFromNewDtoMapper : ObjectMappie<NewCreditCardDto, NewCreditCardRequest>() {
        override fun map(from: NewCreditCardDto) =
            mapping {
                to::showOnDashboard fromProperty NewCreditCardDto::showOnDashboard transform { it ?: true }
            }
    }

    private object CreditCardFromEditDtoMapper : ObjectMappie<EditCreditCardDto, EditCreditCardRequest>() {
        override fun map(from: EditCreditCardDto) = mapping {}
    }
}
