package routing

import authentication.UserPrincipal
import dto.PasswordDto
import dto.ValidationErrorExceptionDto
import exception.PasswordNotFoundException
import exception.UserNotFoundException
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import model.Password
import repository.PasswordRepository
import validation.PasswordDtoValidator
import validation.exception.InvalidPasswordException
import java.util.*

fun Route.password(passwordRepository: PasswordRepository) {
    post {
        val user = context.principal<UserPrincipal>()!!

        val passwordValidator = PasswordDtoValidator()

        val passwordDto = call.receive<PasswordDto>()

        try {
            passwordValidator.validate(passwordDto)

            val password = passwordRepository.createPassword(
                    username = user.username,
                    name = passwordDto.name!!,
                    domain = passwordDto.domain!!,
                    account = passwordDto.account!!
            )

            call.respond(HttpStatusCode.Created, PasswordDto(
                    id = password.id,
                    name = password.name,
                    domain = password.domain,
                    account = password.account
            ))
        } catch (exception: InvalidPasswordException) {
            call.respond(HttpStatusCode.BadRequest, ValidationErrorExceptionDto(
                    message = exception.message!!,
                    property = exception.fieldName
            ))
        } catch (exception: UserNotFoundException) {
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    get {
        val user = context.principal<UserPrincipal>()!!

        try {
            val passwords = passwordRepository.getPasswordsForUser(user.username)

            call.respond(HttpStatusCode.OK, passwords.map { password: Password ->
                PasswordDto(
                        id = password.id,
                        name = password.name,
                        domain = password.domain,
                        account = password.account
                )
            })
        } catch (exception: UserNotFoundException) {
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    get("/{id}") {
        val user = context.principal<UserPrincipal>()!!
        val id = UUID.fromString(call.parameters["id"])

        try {
            val password = passwordRepository.getPassword(username = user.username, id = id)

            call.respond(HttpStatusCode.OK, PasswordDto(
                    id = password.id,
                    name = password.name,
                    domain = password.domain,
                    account = password.account
            ))
        } catch (exception: UserNotFoundException) {
            call.respond(HttpStatusCode.Forbidden)
        } catch (exception: PasswordNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    delete("/{id}") {
        val user = context.principal<UserPrincipal>()!!
        val id = UUID.fromString(call.parameters["id"])

        try {
            passwordRepository.deletePassword(username = user.username, id = id)

            call.respond(HttpStatusCode.OK)
        } catch (exception: UserNotFoundException) {
            call.respond(HttpStatusCode.Forbidden)
        }
    }
}