package br.pucpr.authserver.users.requests

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @NotBlank
    var email: String?,

    @NotBlank
    var password: String?

    @field:NotBlank val phone: String,
    @field:NotBlank val uuid: String
)