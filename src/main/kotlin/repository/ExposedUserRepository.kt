package repository

import com.muquit.libsodiumjna.SodiumLibrary.randomBytes
import com.muquit.libsodiumjna.SodiumUtils
import db.PublicKeys as DbPublicKeys
import db.Token as DbToken
import db.Users as DbUsers
import exception.TokenNotFoundException
import exception.UserNotFoundException
import exception.PublicKeyNotFoundException
import model.PublicKey
import model.Token
import model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import security.PasswordHasher
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.NoSuchElementException

class ExposedUserRepository(
        private val dbUserTable: DbUsers,
        private val dbTokenTable: DbToken,
        private val dbPublicKeyTable: DbPublicKeys,
        private val passwordHasher: PasswordHasher,
        private val tokenTTL: Int,
        private val tokenByteSize: Int
): UserRepository {

    private val mapUserFromDb = { row: ResultRow ->
            User(
                    username = row.get(dbUserTable.username),
                    hashedPassword = row.get(dbUserTable.password).toString(StandardCharsets.US_ASCII)
            )
        }

    private val mapPublicKeyFromDb = { row: ResultRow ->
        PublicKey(
                id = row.get(dbPublicKeyTable.id),
                addedAt = row.get(dbPublicKeyTable.addedAt).toDate(),
                keyString = row.get(dbPublicKeyTable.publicKey)
        )
    }

    override fun registerUser(username: String, plaintextPassword: String) {
        val hashedPassword = passwordHasher.hashPassword(plaintextPassword)

        transaction {
            dbUserTable.insert {
                it[this.username] = username
                it[this.password] = hashedPassword
            }
        }
    }

    override fun getUserByUsername(username: String): User {
        return transaction {
            val dbUsers = dbUserTable.select {
                dbUserTable.username eq username
            }.limit(1).map(mapUserFromDb)

            try {
                dbUsers.first()
            } catch(exception: NoSuchElementException) {
                throw UserNotFoundException(username)
            }
        }
    }

    override fun issueTokenForUser(username: String): Token {
        val cal = Calendar.getInstance()

        val issuedAt = cal.time

        cal.add(Calendar.DATE, tokenTTL)

        val validUntil = cal.time

        val token = Token(
                username = username,
                token = SodiumUtils.binary2Hex(randomBytes(tokenByteSize)),
                issuedAt = issuedAt,
                validUntil = validUntil
        )

        transaction {
            val dbUserCount = dbUserTable.select {
                dbUserTable.username eq token.username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(token.username)
            }

            dbTokenTable.insert {
                it[this.tokenValue] = token.token
                it[this.issueDate] = DateTime(token.issuedAt)
                it[this.validUntil] = DateTime(token.validUntil)
                it[this.user] = token.username
            }
        }

        return token
    }

    override fun getUserForToken(token: String): User {
        return transaction {
            val dbUsers = dbUserTable.innerJoin(dbTokenTable).select {
                dbTokenTable.tokenValue eq token and (dbTokenTable.validUntil greaterEq DateTime.now())
            }.map(mapUserFromDb)

            try {
                dbUsers.first()
            } catch (exception: NoSuchElementException) {
                throw TokenNotFoundException(token)
            }
        }
    }

    override fun getPublicKeys(username: String): List<PublicKey> =
            transaction {
                dbPublicKeyTable.select {
                    dbPublicKeyTable.user eq username
                }.map { mapPublicKeyFromDb(it) }
            }

    override fun addPublicKey(publicKeyStr: String, username: String): PublicKey {
        val publicKey = PublicKey(
                id = UUID.randomUUID(),
                keyString = publicKeyStr,
                addedAt = DateTime.now().toDate()
        )

        transaction {
            val dbUserCount = dbUserTable.select {
                dbUserTable.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            dbPublicKeyTable.insert {
                it[this.id] = publicKey.id
                it[this.publicKey] = publicKey.keyString
                it[this.addedAt] = DateTime(publicKey.addedAt)
                it[this.user] = username
            }
        }

        return publicKey
    }

    override fun getPublicKey(publicKeyStr: String, username: String): PublicKey {
        return transaction {
            val dbPublicKeys = dbPublicKeyTable.select {
                dbPublicKeyTable.publicKey eq publicKeyStr and
                        (dbPublicKeyTable.user eq username)
            }.limit(1).map(mapPublicKeyFromDb)

            try {  dbPublicKeys.first()
            } catch (exception: NoSuchElementException) {
                throw PublicKeyNotFoundException(publicKeyStr)
            }
        }
    }

    override fun getPublicKey(id: UUID, username: String): PublicKey {
        return transaction {
            val dbPublicKeys = dbPublicKeyTable.select {
                dbPublicKeyTable.id eq id and
                        (dbPublicKeyTable.user eq username)
            }.limit(1).map(mapPublicKeyFromDb)

            try {
               dbPublicKeys.first()
            } catch (exception: NoSuchElementException) {
                throw PublicKeyNotFoundException(id)
            }
        }
    }

    override fun revokePublicKey(publicKeyStr: String, username: String) {
        transaction {
            val deletedKey = dbPublicKeyTable.deleteWhere {
                dbPublicKeyTable.publicKey eq publicKeyStr and
                        (dbPublicKeyTable.user eq username)
            }

            if (deletedKey == 0) {
                throw PublicKeyNotFoundException(publicKeyStr)
            }
        }
    }

    override fun revokePublicKey(id: UUID, username: String) {
        transaction {
            val deletedKey = dbPublicKeyTable.deleteWhere {
                dbPublicKeyTable.id eq id and
                        (dbPublicKeyTable.user eq username)
            }

            if (deletedKey == 0) {
                throw PublicKeyNotFoundException(id)
            }
        }
    }
}