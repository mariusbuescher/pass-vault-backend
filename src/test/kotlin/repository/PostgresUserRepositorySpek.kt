package repository

import db.Token
import db.Users
import exception.TokenNotFoundException
import exception.UserNotFoundException
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.sql.Database
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.sqlite.SQLiteDataSource
import org.sqlite.SQLiteException
import security.PasswordHasher
import java.sql.Connection

object PostgresUserRepositorySpek: Spek({
    describe("PostgresUserRepository") {
        var counter = 0

        val dataSource by memoized {
            val dataSource = SQLiteDataSource()
            dataSource.url = "jdbc:sqlite:file:test$counter?mode=memory&cache=shared"
            val con = dataSource.connection

            val dbPrepStream = this.javaClass.classLoader
                    .getResourceAsStream("db/migration/sqlite/UpPostgresUserRepositorySpek.sql")
            val dbPrep = IOUtils.toString(dbPrepStream, StandardCharsets.UTF_8)
            val statements = dbPrep.split("\n\n")
            for (stmt in statements) {
                con.createStatement().execute(stmt)
            }

            counter++

            dataSource
        }

        val userRepository by memoized {
            Database.connect(dataSource)
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

            class NullPasswordHasher: PasswordHasher {
                override fun hashPassword(plaintextPassword: String): ByteArray = plaintextPassword.toByteArray()
                override fun verifyPassword(plaintextPassword: String, hashedPassword: String): Boolean = true
            }

            val passwordHasher = NullPasswordHasher()

            PostgresUserRepository(Users, Token, passwordHasher, 7, 32)
        }

        describe("#registerUser()") {
            it("registers a user and gets it by its username") {
                userRepository.registerUser("test", "1234")

                val user = userRepository.getUserByUsername("test")

                assertEquals("test", user.username)
            }

            it("throws an error when no user is found") {
                assertFailsWith(UserNotFoundException::class, "User with username test not found") {
                    userRepository.getUserByUsername("test")
                }
            }
        }

        describe("#issueToken()") {
            it("issues a token for a user and gets the user by this token") {
                userRepository.registerUser("test", "1234")

                val token = userRepository.issueTokenForUser("test");
                val user = userRepository.getUserForToken(token.token)

                assertEquals("test", user.username)
            }

            it("thows an error when no user is found for the new token") {
                assertFailsWith<UserNotFoundException>("User with username test not found") {
                    userRepository.issueTokenForUser("test")
                }
            }
        }

        describe("#getUserForToken()") {
            it("throws an exception if no valid token was found") {
                userRepository.registerUser("test", "1234")

                userRepository.issueTokenForUser("test")

                assertFailsWith(TokenNotFoundException::class, "Token 1234 not found") {
                    userRepository.getUserForToken("1234")
                }
            }
        }
    }
})