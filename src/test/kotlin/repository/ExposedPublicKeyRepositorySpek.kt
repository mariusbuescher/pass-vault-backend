package repository

import db.PublicKeys
import db.Users
import exception.PublicKeyNotFoundException
import exception.UserNotFoundException
import org.apache.commons.io.IOUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
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

object ExposedPublicKeyRepositorySpek: Spek({
    describe("ExposedPublicKeyRepository") {
        val database by memoized {
            val dataSource = SQLiteDataSource()
            dataSource.url = "jdbc:sqlite:file:publicKeyTest${DateTime.now().millis}?mode=memory&cache=shared"
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

        describe("#addPublicKey") {
            it("throws an error when no user is found for the new key") {
                assertFailsWith<UserNotFoundException>("User with username test not found") {
                    val publicKeyRepository =  ExposedPublicKeyRepository(
                            database = database
                    )

                    publicKeyRepository.addPublicKey("1234", "test")
                }
            }

            it("adds a public key for a user") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                val publicKey = publicKeyRepository.addPublicKey("1234", "test")

                val retrievedPublicKey = publicKeyRepository.getPublicKey(publicKey.id, "test")

                assertEquals("1234", retrievedPublicKey.keyString)
            }
        }

        describe("#getPublicKey()") {
            it("throws an error when the desired public key is not found by ID") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                val id = UUID.randomUUID()

                assertFailsWith<PublicKeyNotFoundException>("Public key with id $id not found") {
                    publicKeyRepository.getPublicKey(id, "test")
                }
            }

            it("throws an error when the desired public key is not found by key string") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                assertFailsWith<PublicKeyNotFoundException>("Public key with id 1234 not found") {
                    publicKeyRepository.getPublicKey("1234", "test")
                }
            }

            it("throws an error when the desired public key is not found with the user") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                publicKeyRepository.addPublicKey("1234", "test")

                assertFailsWith<PublicKeyNotFoundException>("Public key with id 1234 not found") {
                    publicKeyRepository.getPublicKey("1234", "foo")
                }
            }
        }

        describe("#getPublicKeys()") {
            it("returns all public keys for a given user") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                publicKeyRepository.addPublicKey("Key1", "test")
                publicKeyRepository.addPublicKey("Key2", "test")

                val keys = publicKeyRepository.getPublicKeys("test")

                assertEquals(2, keys.count())
                assertEquals("Key1", keys[0].keyString)
                assertEquals("Key2", keys[1].keyString)
            }

            it("returns an empty list when user does not exist") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                val keys = publicKeyRepository.getPublicKeys("test")

                assertEquals(0, keys.count())
            }
        }

        describe("#revokePublicKey()") {
            it("removes a public key with a given ID") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                val publicKey = publicKeyRepository.addPublicKey("1234", "test")

                publicKeyRepository.revokePublicKey(publicKey.id, "test")

                val allPublicKeys = publicKeyRepository.getPublicKeys("test")

                assertEquals(0, allPublicKeys.count())
            }

            it("removes a public key with a given key string") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                publicKeyRepository.addPublicKey("1234", "test")

                publicKeyRepository.revokePublicKey("1234", "test")

                val allPublicKeys = publicKeyRepository.getPublicKeys("test")

                assertEquals(0, allPublicKeys.count())
            }

            it("throws an error when no public key is found for a id") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                val id = UUID.randomUUID()

                assertFailsWith<PublicKeyNotFoundException>("Public key with id $id not found") {
                    publicKeyRepository.revokePublicKey(id, "test")
                }
            }

            it("throws an error when no public key is found for a key string") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                assertFailsWith<PublicKeyNotFoundException>("Public key with 1234 not found") {
                    publicKeyRepository.revokePublicKey("1234", "test")
                }
            }

            it("throws an exception when public key with id is not found for user") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }

                    Users.insert {
                        it[this.username] = "test2"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                val publicKey = publicKeyRepository.addPublicKey("1234", "test2")

                assertFailsWith<PublicKeyNotFoundException>("Public key with id ${publicKey.id} not found") {
                    publicKeyRepository.revokePublicKey(publicKey.id, "test")
                }
            }

            it("throws an exception when public key is not found for user") {
                val publicKeyRepository =  ExposedPublicKeyRepository(
                        database = database
                )

                transaction(database) {
                    Users.insert {
                        it[this.username] = "test"
                        it[this.password] = "1234".toByteArray()
                    }

                    Users.insert {
                        it[this.username] = "test2"
                        it[this.password] = "1234".toByteArray()
                    }
                }

                publicKeyRepository.addPublicKey("1234", "test2")

                assertFailsWith<PublicKeyNotFoundException>("Public key 1234 not found") {
                    publicKeyRepository.revokePublicKey("1234", "test")
                }
            }
        }
    }
})