package repository

import db.PublicKeys
import db.Token
import db.Users
import exception.PublicKeyNotFoundException
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
import security.PasswordHasher
import java.sql.Connection
import java.util.*

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

            PostgresUserRepository(
                    dbUserTable = Users,
                    dbTokenTable = Token,
                    dbPublicKeyTable = PublicKeys,
                    passwordHasher = passwordHasher,
                    tokenTTL = 7,
                    tokenByteSize = 32
            )
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

        describe("#addPublicKey") {
            it("throws an error when no user is found for the new key") {
                assertFailsWith<UserNotFoundException>("User with username test not found") {
                    userRepository.addPublicKey("1234", "test")
                }
            }

            it("adds a public key for a user") {
                userRepository.registerUser("test", "1234")
                val publicKey = userRepository.addPublicKey("1234", "test")

                val retrievedPublicKey = userRepository.getPublicKey(publicKey.id, "test")

                assertEquals("1234", retrievedPublicKey.keyString)
            }
        }

        describe("#getPublicKey()") {
            it("throws an error when the desired public key is not found by ID") {
                userRepository.registerUser("test", "1234")
                val id = UUID.randomUUID()

                assertFailsWith<PublicKeyNotFoundException>("Public key with id ${id.toString()} not found") {
                    userRepository.getPublicKey(id, "test")
                }
            }

            it("throws an error when the desired public key is not found by key string") {
                userRepository.registerUser("test", "1234")

                assertFailsWith<PublicKeyNotFoundException>("Public key with id 1234 not found") {
                    userRepository.getPublicKey("1234", "test")
                }
            }

            it("throws an error when the desired public key is not found with the user") {
                userRepository.registerUser("test", "1234")
                userRepository.addPublicKey("1234", "test")

                assertFailsWith<PublicKeyNotFoundException>("Public key with id 1234 not found") {
                    userRepository.getPublicKey("1234", "foo")
                }
            }
        }

        describe("#getPublicKeys()") {
            it("returns all public keys for a given user") {
                userRepository.registerUser("test", "1234")
                userRepository.addPublicKey("Key1", "test")
                userRepository.addPublicKey("Key2", "test")

                val keys = userRepository.getPublicKeys("test")

                assertEquals(2, keys.count())
                assertEquals("Key1", keys.get(0).keyString)
                assertEquals("Key2", keys.get(1).keyString)
            }

            it("returns an empty list when user does not exist") {
                val keys = userRepository.getPublicKeys("test")

                assertEquals(0, keys.count())
            }
        }

        describe("#revokePublicKey()") {
            it("removes a public key with a given ID") {
                userRepository.registerUser("test", "1234")
                val publicKey = userRepository.addPublicKey("1234", "test")

                userRepository.revokePublicKey(publicKey.id, "test")

                val allPublicKeys = userRepository.getPublicKeys("test")

                assertEquals(0, allPublicKeys.count())
            }

            it("removes a public key with a given key string") {
                userRepository.registerUser("test", "1234")
                userRepository.addPublicKey("1234", "test")

                userRepository.revokePublicKey("1234", "test")

                val allPublicKeys = userRepository.getPublicKeys("test")

                assertEquals(0, allPublicKeys.count())
            }

            it("throws an error when no public key is found for a id") {
                userRepository.registerUser("test", "1234")
                val id = UUID.randomUUID()

                assertFailsWith<PublicKeyNotFoundException>("Public key with id ${id} not found") {
                    userRepository.revokePublicKey(id, "test")
                }
            }

            it("throws an error when no public key is found for a key string") {
                userRepository.registerUser("test", "1234")

                assertFailsWith<PublicKeyNotFoundException>("Public key with 1234 not found") {
                    userRepository.revokePublicKey("1234", "test")
                }
            }

            it("throws an exception when public key with id is not found for user") {
                userRepository.registerUser("test", "1234")
                userRepository.registerUser("test2", "1234")
                val publicKey = userRepository.addPublicKey("1234", "test2")

                assertFailsWith<PublicKeyNotFoundException>("Public key with id ${publicKey.id} not found") {
                    userRepository.revokePublicKey(publicKey.id, "test")
                }
            }

            it("throws an exception when public key is not found for user") {
                userRepository.registerUser("test", "1234")
                userRepository.registerUser("test2", "1234")
                val publicKey = userRepository.addPublicKey("1234", "test2")

                assertFailsWith<PublicKeyNotFoundException>("Public key 1234 not found") {
                    userRepository.revokePublicKey("1234", "test")
                }
            }
        }
    }
})