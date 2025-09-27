package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.events.UserActionEventDto
import com.ynixt.sharedfinances.application.web.mapper.UserActionEventDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.actionevents.ActionEventListenerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/sse")
@Tag(
    name = "Events Stream",
    description = "Events Stream for the logged user",
)
class EventsStreamController(
    private val actionEventListenerService: ActionEventListenerService,
    private val userActionEventDtoMapper: UserActionEventDtoMapper,
) {
    @GetMapping("/user-events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "Events stream for the logged user. This includes groups that user is member")
    fun userEvents(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
    ): Flux<ServerSentEvent<UserActionEventDto>> {
        val userData =
            actionEventListenerService
                .listenUserActions(principalToken.principal.id)
                .map { p ->
                    ServerSentEvent
                        .builder(userActionEventDtoMapper.toDto(p))
                        .event(p.category.toString())
                        .id(p.id.toString())
                        .retry(Duration.ofSeconds(3))
                        .build()
                }

        val groupData =
            actionEventListenerService
                .listenGroupActions(principalToken.principal.id)
                .map { p ->
                    ServerSentEvent
                        .builder(userActionEventDtoMapper.toDto(p))
                        .event(p.category.toString())
                        .id(p.id.toString())
                        .retry(Duration.ofSeconds(3))
                        .build()
                }

        val keepalive =
            Flux
                .interval(Duration.ZERO, Duration.ofSeconds(15))
                .map {
                    ServerSentEvent
                        .builder<UserActionEventDto>()
                        .comment("keepalive")
                        .build()
                }

        return userData.mergeWith(groupData).mergeWith(keepalive)
    }
}
