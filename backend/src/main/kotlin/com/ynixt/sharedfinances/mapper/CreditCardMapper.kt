package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.CreditCard
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardDto
import com.ynixt.sharedfinances.model.dto.creditcard.UpdateCreditCardDto
import org.mapstruct.Mapper
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy


@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface CreditCardMapper {
    fun toDto(creditCard: CreditCard?): CreditCardDto?
    fun toDtoList(creditCard: List<CreditCard>): List<CreditCardDto>
    fun update(@MappingTarget creditCard: CreditCard?, updateCreditCardDto: UpdateCreditCardDto?)
}
