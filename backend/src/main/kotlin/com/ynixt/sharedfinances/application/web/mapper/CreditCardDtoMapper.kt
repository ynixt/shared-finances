package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.CreditCardForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.EditCreditCardDto
import com.ynixt.sharedfinances.application.web.dto.wallet.creditCard.NewCreditCardDto
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest

interface CreditCardDtoMapper {
    fun toDto(from: CreditCard): CreditCardDto

    fun fromDto(from: CreditCardDto): CreditCard

    fun fromNewDtoToNewRequest(from: NewCreditCardDto): NewCreditCardRequest

    fun fromEditDtoToEditRequest(from: EditCreditCardDto): EditCreditCardRequest

    fun toAssociateDto(from: CreditCard): CreditCardForGroupAssociateDto
}
