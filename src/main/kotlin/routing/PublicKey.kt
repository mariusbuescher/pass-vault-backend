package routing

import authentication.UserPrincipal
import dto.PublicKey
import dto.ValidationErrorExceptionDto
import exception.PublicKeyNotFoundException
import io.ktor.application.call
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import repository.PublicKeyRepository
import validation.PublicKeyDtoValidator
import validation.exception.InvalidPublicKeyException
import java.util.*

fun Route.publicKey(publicKeyRepository: PublicKeyRepository) {
    post {
        val user = context.principal<UserPrincipal>()!!

        val publicKeyValidator = PublicKeyDtoValidator()

        val key = call.receive<PublicKey>()

        try {
            publicKeyValidator.validate(key)

            val publicKey = publicKeyRepository.addPublicKey(
                    publicKeyStr = key.key!!,
                    username = user.username
            )

            call.respond(HttpStatusCode.Created, PublicKey(
                    id = publicKey.id,
                    addedAt = publicKey.addedAt,
                    key = publicKey.keyString
            ))
        } catch (exception: InvalidPublicKeyException) {
            call.respond(HttpStatusCode.BadRequest, ValidationErrorExceptionDto(
                    message = exception.message!!,
                    property = exception.fieldName
            ))
        } catch (exception: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get {
        val user = context.principal<UserPrincipal>()!!

        call.respond(HttpStatusCode.OK, publicKeyRepository.getPublicKeys(username = user.username).map {
            PublicKey(
                    id = it.id,
                    addedAt = it.addedAt,
                    key = it.keyString
            )
        })
    }

    get("/{id}") {
        val user = context.principal<UserPrincipal>()!!

        val id = UUID.fromString(call.parameters["id"])
        try {
            val publicKey = publicKeyRepository.getPublicKey(id = id, username = user.username)

            call.respond(HttpStatusCode.OK, PublicKey(
                    id = publicKey.id,
                    addedAt = publicKey.addedAt,
                    key = publicKey.keyString
            ))
        } catch (exception: PublicKeyNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    delete("/{id}") {
        val user = context.principal<UserPrincipal>()!!

        val id = UUID.fromString(call.parameters["id"])

        try {
            publicKeyRepository.revokePublicKey(id = id, username = user.username)
        } catch (exception: PublicKeyNotFoundException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}