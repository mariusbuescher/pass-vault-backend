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
import routing.login
import routing.password
import routing.publicKey
import routing.register
import security.SodiumPasswordHasher
import validation.PasswordDtoValidator
import validation.PublicKeyDtoValidator
import validation.UserDtoValidator
import validation.exception.InvalidPasswordException
import validation.exception.InvalidPublicKeyException
import validation.exception.InvalidUserException
import java.util.*
import security.SodiumTokenFactory

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
    val tokenFactory = SodiumTokenFactory(dotenv.get("APP_AUTH_TOKEN_SIZE")?.toInt() ?: 256)

    val userRepository = ExposedUserRepository(
            database = database,
            passwordHasher = passwordHasher,
            tokenFactory = tokenFactory,
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
        register(userRepository)

        authenticate("jsonUser") {
            login(userRepository)
        }

        authenticate("tokenUser") {
            route("/public-key") {
                publicKey(publicKeyRepository)
            }

            route("/password") {
                password(passwordRepository)
            }
        }
    }
}