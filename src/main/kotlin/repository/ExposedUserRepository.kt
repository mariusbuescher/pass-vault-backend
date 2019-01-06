package repository

import db.PublicKeys as DbPublicKeys
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
import security.TokenFactory
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.NoSuchElementException

class ExposedUserRepository(
        private val database: Database,
        private val passwordHasher: PasswordHasher,
        private val tokenTTL: Int,
        private val tokenFactory: TokenFactory
): UserRepository {

    private val mapUserFromDb = { row: ResultRow ->
            User(
                    username = row.get(DbUsers.username),
                    hashedPassword = row.get(DbUsers.password).toString(StandardCharsets.US_ASCII)
            )
        }

    override fun registerUser(username: String, plaintextPassword: String) {
        val hashedPassword = passwordHasher.hashPassword(plaintextPassword)

        transaction(database) {
            DbUsers.insert {
                it[this.username] = username
                it[this.password] = hashedPassword
            }
        }
    }

    override fun getUserByUsername(username: String): User {
        return transaction(database) {
            val dbUsers = DbUsers.select {
                DbUsers.username eq username
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
                token = tokenFactory.generateRandomToken(),
                issuedAt = issuedAt,
                validUntil = validUntil
        )

        transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq token.username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(token.username)
            }

            DbToken.insert {
                it[this.tokenValue] = token.token
                it[this.issueDate] = DateTime(token.issuedAt)
                it[this.validUntil] = DateTime(token.validUntil)
                it[this.user] = token.username
            }
        }

        return token
    }

    override fun getUserForToken(token: String): User {
        return transaction(database) {
            val dbUsers = DbUsers.innerJoin(DbToken).select {
                DbToken.tokenValue eq token and (DbToken.validUntil greaterEq DateTime.now())
            }.map(mapUserFromDb)

            try {
                dbUsers.first()
            } catch (exception: NoSuchElementException) {
                throw TokenNotFoundException(token)
            }
        }
    }
}