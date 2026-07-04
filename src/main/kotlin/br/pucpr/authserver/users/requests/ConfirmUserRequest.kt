package br.pucpr.authserver.users.requests

import jakarta.validation.constraints.NotBlank

data class ConfirmUserRequest(

    @field:NotBlank
    val phone: String,

    @field:NotBlank
    val uuid: String,

    @field:NotBlank
    val code: String

)