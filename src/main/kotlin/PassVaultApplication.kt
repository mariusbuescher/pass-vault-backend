import authentication.JsonUserAuthenticationProvider
import authentication.TokenUserAuthenticationProvider
import authentication.UserPrincipal
import com.muquit.libsodiumjna.SodiumLibrary
import dto.*
import exception.PasswordNotFoundException
import exception.PublicKeyNotFoundException
import exception.UserNotFoundException
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.principal
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import model.Password
import org.jetbrains.exposed.sql.Database
import org.postgresql.ds.PGSimpleDataSource
import repository.ExposedPasswordRepository
import repository.ExposedPublicKeyRepository
import repository.ExposedUserRepository
import security.SodiumPasswordHasher
import validation.PasswordDtoValidator
import validation.PublicKeyDtoValidator
import validation.UserDtoValidator
import validation.exception.InvalidPasswordException
import validation.exception.InvalidPublicKeyException
import validation.exception.InvalidUserException
import java.util.*

fun Application.main() {
    val dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load()

    SodiumLibrary.setLibraryPath(dotenv.get("SODIUM_LIBRARY_PATH") ?: "/usr/lib/libsodium.so")
    SodiumLibrary.libsodiumVersionString()

    val secret = dotenv.get("APP_AUTH_SECRET") ?: ""

    val dataSource = PGSimpleDataSource()
    dataSource.setURL(dotenv.get("APP_DB_URL") ?: "jdbc:postgresql://localhost:5432/pass_vault?user=postgres")

    val database = Database.connect(dataSource)

    val passwordHasher = SodiumPasswordHasher(secret)

    val userRepository = ExposedUserRepository(
            database = database,
            passwordHasher = passwordHasher,
            tokenByteSize = dotenv.get("APP_AUTH_TOKEN_SIZE")?.toInt() ?: 256,
            tokenTTL = dotenv.get("APP_AUTH_TOKEN_TTL")?.toInt() ?: 14
    )

    val publicKeyRepository = ExposedPublicKeyRepository(
            database = database
    )

    val passwordRepository = ExposedPasswordRepository(
            database = database
    )

    install(ContentNegotiation) {
        jackson {  }
    }

    install(Authentication) {
        register(JsonUserAuthenticationProvider(
                "jsonUser",
                userRepository,
                passwordHasher
        ))

        register(TokenUserAuthenticationProvider(
                "tokenUser",
                userRepository
        ))
    }

    routing {
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

        authenticate("jsonUser") {
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

        authenticate("tokenUser") {
            route("/public-key") {
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

            route("/password") {
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
        }
    }
}