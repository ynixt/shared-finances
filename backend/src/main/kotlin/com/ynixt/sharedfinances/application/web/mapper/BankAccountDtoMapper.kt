package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.BankAccountForGroupAssociateDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.EditBankAccountDto
import com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.domain.models.bankaccount.BankAccount
import com.ynixt.sharedfinances.domain.models.bankaccount.EditBankAccountRequest
import com.ynixt.sharedfinances.domain.models.bankaccount.NewBankAccountRequest

interface BankAccountDtoMapper {
    fun toDto(from: BankAccount): BankAccountDto

    fun fromDto(from: BankAccountDto): BankAccount

    fun fromNewDtoToNewRequest(from: NewBankAccountDto): NewBankAccountRequest

    fun fromEditDtoToEditRequest(from: EditBankAccountDto): EditBankAccountRequest

    fun toAssociateDto(from: BankAccount): BankAccountForGroupAssociateDto
}
