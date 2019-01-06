import authentication.JsonUserAuthenticationProvider
import authentication.TokenUserAuthenticationProvider
import com.muquit.libsodiumjna.SodiumLibrary
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.*
import io.ktor.util.KtorExperimentalAPI
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
import security.SodiumTokenFactory

@UseExperimental(KtorExperimentalAPI::class)
fun Application.main() {
    val applicationConfig = environment.config.config("passVault")

    SodiumLibrary.setLibraryPath(applicationConfig.property("sodium.libraryPath").getString())
    SodiumLibrary.libsodiumVersionString()

    val secret = applicationConfig.property("auth.secret").getString()

    val dataSource = PGSimpleDataSource()
    dataSource.setURL(
            applicationConfig.property("db.url").getString()
    )

    val database = Database.connect(dataSource)

    val passwordHasher = SodiumPasswordHasher(secret)
    val tokenFactory = SodiumTokenFactory(applicationConfig.property("auth.token.size").getString().toInt())

    val userRepository = ExposedUserRepository(
            database = database,
            passwordHasher = passwordHasher,
            tokenFactory = tokenFactory,
            tokenTTL = applicationConfig.property("auth.token.ttl").getString().toInt()
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