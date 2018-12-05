package authentication

import dto.UserDto
import exception.UserNotFoundException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import repository.UserRepository
import security.PasswordHasher
import validation.UserDtoValidator
import validation.exception.InvalidUserException

class JsonUserAuthenticationProvider(
        name: String?,
        private val userRepository: UserRepository,
        private val passwordHasher: PasswordHasher
): AuthenticationProvider(name) {

    private val authenticationFunction: suspend ApplicationCall.(UserDto) -> Principal? = { credentials ->
        val user = userRepository.getUserByUsername(credentials.username!!)
        val plaintextPassword = credentials.password!!

        val isValid = passwordHasher.verifyPassword(
                plaintextPassword,
                user.hashedPassword
        )

        if (isValid) UserPrincipal(user.username) else null
    }

    init {
        val validate = authenticationFunction

        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            val userDto = call.receive<UserDto>()

            val userValidator = UserDtoValidator()

            try {
                userValidator.validate(userDto)

                val principal = userDto.let { validate(call, it) }

                if (principal !== null) {
                    context.principal(principal)
                } else {
                    context.challenge("JsonAuth", AuthenticationFailedCause.InvalidCredentials) {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            } catch (exception: InvalidUserException) {
                context.challenge("JsonAuth", AuthenticationFailedCause.NoCredentials) {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (exception: UserNotFoundException) {
                context.challenge("JsonAuth", AuthenticationFailedCause.InvalidCredentials) {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}