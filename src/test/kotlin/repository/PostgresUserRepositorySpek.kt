package repository

import com.muquit.libsodiumjna.SodiumLibrary
import exception.TokenNotFoundException
import exception.UserNotFoundException
import io.github.cdimascio.dotenv.Dotenv
import org.apache.commons.io.IOUtils
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

object PostgresUserRepositorySpek: Spek({
    describe("PostgresUserRepository") {
        val dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load()

        SodiumLibrary.setLibraryPath(dotenv.get("SODIUM_LIBRARY_PATH") ?: "/usr/lib/libsodium.so")

        val secret = "1234IsNotReallyASecret"

        val connection by memoized {
            val con = DriverManager.getConnection("jdbc:sqlite:file::memory:?allowMultiQueries=true")
            val dbPrepStream = this.javaClass.classLoader
                    .getResourceAsStream("db/migration/sqlite/UpPostgresUserRepositorySpek.sql")

            val dbPrep = IOUtils.toString(dbPrepStream, StandardCharsets.UTF_8)
            val statements = dbPrep.split("\n\n")
            for (stmt in statements) {
                con.createStatement().execute(stmt)
            }

            con
        }
        val userRepository by memoized { PostgresUserRepository(connection, 7, 32, secret) }

        afterEach {
            connection.close()
        }

        it("registers a user and gets it by its username") {
            userRepository.registerUser("test", "1234")

            val user = userRepository.getUserByUsername("test")

            assertEquals("test", user.username)
        }

        it("throws an error when no user is found") {
            assertFailsWith(UserNotFoundException::class, "User with username test not found") {
                userRepository.getUserByUsername("1234")
            }
        }

        it("issues a token for a user and gets the user by this token") {
            userRepository.registerUser("test", "1234")

            val token = userRepository.issueTokenForUser("test");
            val user = userRepository.getUserForToken(token.token)

            assertEquals("test", user.username)
        }

        it("throws an exception if no valid token was found") {
            userRepository.registerUser("test", "1234")

            userRepository.issueTokenForUser("test")

            assertFailsWith(TokenNotFoundException::class, "Token 1234 not found") {
                userRepository.getUserForToken("1234")
            }
        }
    }
})