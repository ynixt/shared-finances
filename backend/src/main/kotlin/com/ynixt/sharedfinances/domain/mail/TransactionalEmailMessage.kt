package com.ynixt.sharedfinances.domain.mail

data class TransactionalEmailMessage(
    val toAddress: String,
    val subject: String,
    val textBody: String,
    val htmlBody: String? = null,
)
