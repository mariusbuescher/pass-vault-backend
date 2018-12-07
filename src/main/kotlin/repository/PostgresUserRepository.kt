package repository

import com.muquit.libsodiumjna.SodiumLibrary.randomBytes
import com.muquit.libsodiumjna.SodiumUtils
import db.Token as DbToken
import db.Users as DbUsers
import exception.TokenNotFoundException
import exception.UserNotFoundException
import model.Token
import model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import security.PasswordHasher
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.NoSuchElementException

class PostgresUserRepository(
        private val dbUserTable: DbUsers,
        private val dbTokenTable: DbToken,
        private val passwordHasher: PasswordHasher,
        private val tokenTTL: Int,
        private val tokenByteSize: Int
): UserRepository {
    override fun registerUser(username: String, plaintextPassword: String) {
        val hashedPassword = passwordHasher.hashPassword(plaintextPassword)

        transaction {
            dbUserTable.insert {
                it[this.username] = username
                it[this.password] = hashedPassword
            }
        }
    }

    private fun mapUserFromDb(row: ResultRow): User = User(
            username = row.get(dbUserTable.username),
            hashedPassword = row.get(dbUserTable.password).toString(StandardCharsets.US_ASCII)
    )

    override fun getUserByUsername(username: String): User {
        return transaction {
            val dbUsers = dbUserTable.select {
                dbUserTable.username eq username
            }.limit(1).map { mapUserFromDb(it) }

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
            }.map { mapUserFromDb(it) }

            try {
                dbUsers.first()
            } catch (exception: NoSuchElementException) {
                throw TokenNotFoundException(token)
            }
        }
    }
}