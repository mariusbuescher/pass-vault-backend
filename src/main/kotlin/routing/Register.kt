package routing

import dto.UserDto
import dto.ValidationErrorExceptionDto
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import repository.UserRepository
import validation.UserDtoValidator
import validation.exception.InvalidUserException

fun Route.register(userRepository: UserRepository) {
    post("/register") {
        val user = call.receive<UserDto>()

        val userValidator = UserDtoValidator()

        try {
            userValidator.validate(user)

            userRepository.registerUser(user.username!!, user.password!!)

            call.respond(HttpStatusCode.Created)
        } catch (exception: InvalidUserException) {
            call.respond(
                    HttpStatusCode.BadRequest,
                    ValidationErrorExceptionDto(exception.message!!, exception.fieldName)
            )
        } catch (exception: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}