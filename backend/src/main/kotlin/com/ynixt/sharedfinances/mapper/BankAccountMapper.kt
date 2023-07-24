package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.BankAccount
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountDto
import org.mapstruct.Mapper

@Mapper
interface BankAccountMapper {
    fun toDto(bankAccount: BankAccount?): BankAccountDto?
    fun toDtoList(bankAccount: List<BankAccount>?): List<BankAccountDto>?
}
