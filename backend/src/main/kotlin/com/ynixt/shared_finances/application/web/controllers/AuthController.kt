package com.ynixt.shared_finances.application.web.controllers

import com.ynixt.shared_finances.application.config.OnlyServiceSecretAllowed
import com.ynixt.shared_finances.domain.models.dto.CreateUserRequestDto
import com.ynixt.shared_finances.domain.services.UserService
import kotlinx.coroutines.reactor.mono
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

data class IdentityCreatedRequest(
    val identity: Map<String, Any>,
    val flow_type: String
)

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService
) {
    @PostMapping("/identity-created")
    @OnlyServiceSecretAllowed
    fun onIdentityCreated(
        @RequestBody payload: IdentityCreatedRequest,
    ): Mono<ResponseEntity<Void>> {
        val id       = payload.identity["id"] as String
        val traits   = (payload.identity["traits"] as Map<String, Any>)
        val name     = traits["name"] as Map<String, String>

        val userRequest = CreateUserRequestDto(
            uid = id,
            firstName  = name["firstName"] as String,
            lastName   = name["lastName"] as String,
            email      = traits["email"] as String,
            lang       = traits["lang"]  as String
        )

        return mono {
            userService.createUser(userRequest)
        }.thenReturn(ResponseEntity.ok().build())
    }
}