package br.pucpr.authserver.users

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exception.UnauthorizedException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.integration.quotes.QuoteClient
import br.pucpr.authserver.integration.sms.SMSClient
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.security.Jwt
import br.pucpr.authserver.users.responses.LoginResponse
import br.pucpr.authserver.users.responses.UserResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.random.Random

@Service
class UserService(
    val repository: UserRepository,
    val roleRepository: RoleRepository,
    val avatarService: AvatarService,
    val jwt: Jwt,
    val quoteClient: QuoteClient,
    val smsClient: SMSClient,
) {
    fun insert(user: User): User {
        if (repository.findByEmail(user.email) != null) {
            throw BadRequestException("User already exists")
        }
        if (user.bio.isEmpty()) {
            user.bio = quoteClient.randomQuote()?.text ?: ""
        }
        if (user.phone.length == 14) {
            val code = Random.nextInt(1000, 9999)
            smsClient.send(user, "Hello ${user.name}! Here's your AuthServer code: $code", true)
        }
        return repository.save(user)
    }

    fun findAll(dir: SortDir = SortDir.ASC) = when (dir) {
        SortDir.ASC -> repository.findAll(Sort.by("name").ascending())
        SortDir.DESC -> repository.findAll(Sort.by("name").descending())
    }

    fun findByIdOrNull(id: Long) = repository.findByIdOrNull(id)
    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    fun delete(id: Long) {
        val user = findById(id)
        if (user.isAdmin() && repository.findByRole("ADMIN").size == 1) {
            throw BadRequestException("Cannot delete the last admin")
        }
        repository.delete(user)
        log.info("User $id deleted successfully")
    }

    fun findByRole(role: String) = repository.findByRole(role)

    fun addRole(id: Long, roleName: String): Boolean {
        val upperRole = roleName.uppercase()
        val user = findById(id)
        if (user.roles.any { it.name == upperRole }) return false

        val role = roleRepository.findByName(upperRole) ?: throw BadRequestException("Role $upperRole not found")

        user.roles.add(role)
        repository.save(user)
        log.info("User $id successfully added to role $role")
        return true
    }

    fun update(id: Long, name: String): User? {
        val user = findById(id)
        if (user.name == name) {
            return null
        }
        user.name = name
        repository.save(user)
        return user
    }

    fun login(request: LoginRequest): UserResponse? {
        val user = repository.findByPhone(request.phone)

        if (user != null && user.isActive && user.deviceUuid == request.uuid) {
            // Login bem sucedido (telefone e uuid conferem)
            return UserResponse(user) // Assumindo que você converte User para UserResponse aqui
        }

        // Caso telefone não exista, ou UUID seja diferente:
        val code = generateConfirmationCode()

        val targetUser = user ?: User(phone = request.phone)
        targetUser.confirmationCode = code
        // Não salva o UUID novo ainda, espera a confirmação

        repository.save(targetUser)

        // Envia o SMS
        smsClient.sendSms(request.phone, "Seu código de confirmação é: $code")

        return null // Sinaliza para o Controller retornar 202
    }

    fun confirmUser(request: ConfirmUserRequest): UserResponse {
        val user = repository.findByPhone(request.phone)

        // Retorna 404 se não houver código, se o código não bater, ou se o usuário não existir
        if (user == null || user.confirmationCode == null || user.confirmationCode != request.code) {
            throw NotFoundException("Código de confirmação inválido ou não encontrado para este número.")
        }

        // Código correto: ativa usuário e atualiza UUID
        user.isActive = true
        user.deviceUuid = request.uuid
        user.confirmationCode = null // Limpa o código após o uso

        val savedUser = repository.save(user)
        return UserResponse(savedUser)
    }

    fun update(id: Long, request: UpdateUserRequest): UserResponse {
        val user = repository.findById(id).orElseThrow { NotFoundException("Usuário não encontrado") }

        request.name?.let { user.name = it }
        request.description?.let { user.description = it }

        return UserResponse(repository.save(user))
    }

    private fun generateConfirmationCode(): String {
        return (100000..999999).random().toString() // Gera um código de 6 dígitos
    }

    fun saveAvatar(id: Long, avatar: MultipartFile): String {
        val user = findById(id)
        user.avatar = avatarService.save(user, avatar)
        repository.save(user)
        return avatarService.urlFor(user.avatar)
    }

    fun toResponse(user: User) =
        UserResponse(user, avatarService.urlFor(user.avatar))

    companion object {
        val log = LoggerFactory.getLogger(UserService::class.java)
    }
}