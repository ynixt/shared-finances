package com.ynixt.sharedfinances.config.security

import com.ynixt.sharedfinances.entity.UserPrincipal
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor


@Configuration
class ChannelTokenInterceptor(
    private val securityService: SecurityService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)!!
        if (StompCommand.CONNECT == accessor.command) {
            val user = securityService.getUser(accessor)

            if (user != null) {
                accessor.user = UserPrincipal(user)
                return message
            }

            return null
        }

        return message
    }

    @Bean
    fun getChannelInterceptor(securityService: SecurityService): ChannelTokenInterceptor {
        return ChannelTokenInterceptor(securityService)
    }
}
