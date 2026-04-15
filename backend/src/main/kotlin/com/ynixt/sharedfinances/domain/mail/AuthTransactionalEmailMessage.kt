package com.ynixt.sharedfinances.domain.mail

data class AuthTransactionalEmailMessage(
    val toAddress: String,
    val subject: String,
    val textBody: String,
    val htmlBody: String? = null,
)
