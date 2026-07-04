package br.pucpr.authserver.users

import br.pucpr.authserver.roles.Role
import jakarta.persistence.*

@Entity
@Table(name = "UserTable")
class User (
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var bio: String = "",

    // O telefone deve aparecer apenas uma vez.
    @Column(unique = true)
    var phone: String? = null,

    var deviceUuid: String? = null,

    var confirmationCode: String? = null,

    var isActive: Boolean = false,

    // A vírgula no final desta linha é obrigatória!
    var description: String? = null,

    @ManyToMany
    @JoinTable(
        name = "UserRole",
        joinColumns = [JoinColumn(name = "idUser")],
        inverseJoinColumns = [JoinColumn(name = "idRole")]
    )
    var roles: MutableSet<Role> = mutableSetOf(),

    var avatar: String = AvatarService.DEFAULT_AVATAR
) {
    @Transient
    fun isAdmin() = roles.any { it.name == "ADMIN" }
}