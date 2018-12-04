import authentication.JsonUserAuthenticationProvider
import authentication.TokenUserAuthenticationProvider
import authentication.UserPrincipal
import com.muquit.libsodiumjna.SodiumLibrary
import dto.TokenDto
import dto.UserDto
import dto.ValidationErrorExceptionDto
import exception.UserNotFoundException
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.response.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receive
import org.jetbrains.exposed.sql.Database
import org.postgresql.ds.PGSimpleDataSource
import repository.PostgresUserRepository
import validation.UserDtoValidator
import validation.exception.InvalidUserException
import java.sql.DriverManager
import javax.sql.DataSource

fun main(args: Array<String>) {
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

    Database.connect(dataSource)

    val userRepository = PostgresUserRepository(
            tokenByteSize = dotenv.get("APP_AUTH_TOKEN_SIZE")?.toInt() ?: 256,
            tokenTTL = dotenv.get("APP_AUTH_TOKEN_TTL")?.toInt() ?: 14,
            secret = secret
    )

    val server = embeddedServer(Netty, port = dotenv.get("APP_HTTP_PORT")?.toInt() ?: 8088) {
        install(ContentNegotiation) {
            jackson {  }
        }

        install(Authentication) {
            register(JsonUserAuthenticationProvider(
                    "jsonUser",
                    userRepository,
                    secret
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
                get("/test") {
                    call.respond("It worked!")
                }
            }
        }
    }
    server.start(true)
}
