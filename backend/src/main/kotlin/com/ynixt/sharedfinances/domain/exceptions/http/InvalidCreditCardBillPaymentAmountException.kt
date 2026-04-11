package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus
import java.math.BigDecimal

class InvalidCreditCardBillPaymentAmountException(
    amount: BigDecimal,
    remaining: BigDecimal,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.invalidCreditCardBillPaymentAmount",
        alternativeMessage =
            "Invalid credit card bill payment amount ${amount.stripTrailingZeros().toPlainString()} for remaining balance ${remaining.stripTrailingZeros().toPlainString()}",
        argsI18n =
            mapOf(
                "amount" to amount.stripTrailingZeros().toPlainString(),
                "remaining" to remaining.stripTrailingZeros().toPlainString(),
            ),
    )
