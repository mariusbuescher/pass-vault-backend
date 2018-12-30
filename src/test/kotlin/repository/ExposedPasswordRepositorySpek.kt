package repository

import db.Passwords
import db.Users
import exception.PasswordNotFoundException
import exception.UserNotFoundException
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.sqlite.SQLiteDataSource
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

object ExposedPasswordRepositorySpek: Spek({
    describe("Password repository") {
        val database by memoized<Database> {
            val dataSource = SQLiteDataSource()
            dataSource.url = "jdbc:sqlite:file:passwordTest${DateTime.now().millis}?mode=memory&cache=shared"
            val con = dataSource.connection

            val dbPrepStream = this.javaClass.classLoader
                    .getResourceAsStream("db/migration/sqlite/UpSqlite.sql")
            val dbPrep = IOUtils.toString(dbPrepStream, StandardCharsets.UTF_8)
            val statements = dbPrep.split("\n\n")
            for (stmt in statements) {
                con.createStatement().execute(stmt)
            }

            val database = Database.connect(datasource = dataSource)
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

            database
        }

        describe("#createPassword") {
            it("Should create a password when a user exists") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val password = passwordRepository.createPassword("test", "test password", "example.com", "test1")

                assertEquals("test password", password.name)
                assertEquals("example.com", password.domain)
                assertEquals("test1", password.account)
            }

            it("Should throw an exception when user does not exist") {
                val passwordRepository = ExposedPasswordRepository(database)

                assertFailsWith<UserNotFoundException>("User with username test not found.") {
                    passwordRepository.createPassword("test", "test password", "example.com", "test1")
                }
            }
        }

        describe("#getPasswordsForUser") {
            it("Should return all passwords for a given user") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val password1 = passwordRepository.createPassword("test", "test password 1", "example.com", "test1")
                val password2 = passwordRepository.createPassword("test", "test password 2", "example.com", "test2")

                val passwords = passwordRepository.getPasswordsForUser("test")

                assertSame(2, passwords.count())
                assertEquals(password1.id, passwords.get(0).id)
                assertEquals(password2.id, passwords.get(1).id)
            }

            it("should return an empty list when user has no stored passwords") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val passwords = passwordRepository.getPasswordsForUser("test")

                assertSame(0, passwords.count())
            }

            it("should throw an exception when user does not exist") {
                val passwordRepository = ExposedPasswordRepository(database)

                assertFailsWith<UserNotFoundException>("User with username test not found.") {
                    passwordRepository.getPasswordsForUser("test")
                }
            }
        }

        describe("#getPassword") {
            it("should return the requested password for the user") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val password = passwordRepository.createPassword("test", "test password 1", "example.com", "test1")

                val resultPassword = passwordRepository.getPassword("test", password.id)

                assertEquals(password.id, resultPassword.id)
                assertEquals(password.name, resultPassword.name)
                assertEquals(password.domain, resultPassword.domain)
                assertEquals(password.account, resultPassword.account)
            }

            it("should throw an exception when user is not found") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val password = passwordRepository.createPassword("test", "test password 1", "example.com", "test1")

                assertFailsWith<UserNotFoundException>("User with username test1 not found.") {
                    passwordRepository.getPassword("test1", password.id)
                }
            }

            it("should throw an error when the requested password is not found") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val randomId = UUID.randomUUID()

                passwordRepository.createPassword("test", "test password 1", "example.com", "test1")

                assertFailsWith<PasswordNotFoundException>("Password with id $randomId not found") {
                    passwordRepository.getPassword("test", randomId)
                }
            }
        }

        describe("#deletePassword") {
            it("should delete a given password") {
                transaction(database) {
                    Users.insert {
                        it[Users.username] = "test"
                        it[Users.password] = "1234".toByteArray()
                    }
                }

                val passwordRepository = ExposedPasswordRepository(database)

                val password = passwordRepository.createPassword("test", "test password 1", "example.com", "test1")

                passwordRepository.deletePassword("test", password.id)

                val passwordCount = transaction(database) {
                    Passwords.select {
                        Passwords.id eq password.id
                    }.count()
                }

                assertSame(0, passwordCount)
            }

            it("throws an exception when user does not exist") {
                val passwordRepository = ExposedPasswordRepository(database)

                assertFailsWith<UserNotFoundException>("User with username foo not found.") {
                    passwordRepository.deletePassword("foo", UUID.randomUUID())
                }
            }
        }
    }
})