package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.EditBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import com.ynixt.sharedfinances.domain.models.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.NewBankAccountRequest

interface BankAccountDtoMapper {
    fun toDto(from: BankAccount): BankAccountDto

    fun fromDto(from: BankAccountDto): BankAccount

    fun fromNewDtoToNewRequest(from: NewBankAccountDto): NewBankAccountRequest

    fun fromEditDtoToEditRequest(from: EditBankAccountDto): EditBankAccountRequest
}
