package br.pucpr.authserver.users.responses

import br.pucpr.authserver.users.User

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val bio: String,
    val avatar: String,
    val description: String?,
    val phone: String?,
    val isActive: Boolean
) {
    // O construtor pega os dados da entidade User e joga para o Response
    constructor(user: User, avatarUrl: String = "") : this(
        id = user.id!!,
        email = user.email,
        name = user.name,
        bio = user.bio,
        avatar = avatarUrl.ifEmpty { user.avatar },
        description = user.description,
        phone = user.phone,
        isActive = user.isActive
    )
}