package com.ynixt.sharedfinances.application.web.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserDto(
    @field:NotBlank(message = "apiErrors.registration.emailInvalid")
    @field:Size(min = 1, max = 320, message = "apiErrors.registration.emailSize")
    @field:Email(message = "apiErrors.registration.emailInvalid")
    val email: String,
    @field:NotBlank(message = "apiErrors.registration.firstNameSize")
    @field:Size(min = 1, max = 20, message = "apiErrors.registration.firstNameSize")
    val firstName: String,
    @field:NotBlank(message = "apiErrors.registration.lastNameSize")
    @field:Size(min = 1, max = 20, message = "apiErrors.registration.lastNameSize")
    val lastName: String,
    @field:NotBlank(message = "apiErrors.generic.fieldInvalid")
    @field:Size(min = 1, max = 8, message = "apiErrors.generic.fieldInvalid")
    val lang: String,
    @field:NotBlank(message = "apiErrors.generic.fieldInvalid")
    @field:Size(min = 1, max = 3, message = "apiErrors.generic.fieldInvalid")
    val defaultCurrency: String,
    @field:NotBlank(message = "apiErrors.generic.fieldInvalid")
    @field:Size(min = 1, max = 255, message = "apiErrors.generic.fieldInvalid")
    val tmz: String,
    val removeAvatar: Boolean = false,
    val getFromGravatar: Boolean = false,
    val darkMode: Boolean = false,
)
