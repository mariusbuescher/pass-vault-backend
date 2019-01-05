package routing

import authentication.UserPrincipal
import dto.TokenDto
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import repository.UserRepository

fun Route.login(userRepository: UserRepository) {
    post("/login") {
        val user = call.authentication.principal<UserPrincipal>()
        val token = userRepository.issueTokenForUser(user!!.username)
        call.respond(HttpStatusCode.Created, TokenDto(
                token = token.token,
                issuedAt = token.issuedAt,
                validUntil = token.validUntil
        ))
    }
}