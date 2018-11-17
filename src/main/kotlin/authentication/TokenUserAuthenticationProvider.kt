package authentication

import exception.TokenNotFoundException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import repository.UserRepository

class TokenUserAuthenticationProvider(
        name: String?,
        private val userRepository: UserRepository,
        private val headerName: String = "X-Authentication"
): AuthenticationProvider(name) {
    private val authenticationFunction: suspend ApplicationCall.(String) -> Principal? = { token ->
        try {
            val user = userRepository.getUserForToken(token)
            UserPrincipal(user.username)
        } catch (exception: TokenNotFoundException) {
            null
        }
    }

    init {
        val validate = authenticationFunction
        val header = headerName

        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            val token = call.request.header(header)

            val principal = token?.let { validate(call, it) }

            if (principal != null) {
                context.principal(principal)
            } else {
                context.challenge("TokenAuth", AuthenticationFailedCause.InvalidCredentials) {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}