package com.ynixt.sharedfinances.domain.services.mail

import com.ynixt.sharedfinances.domain.mail.TransactionalEmailMessage

interface TransactionalEmailSender {
    suspend fun send(message: TransactionalEmailMessage)
}
