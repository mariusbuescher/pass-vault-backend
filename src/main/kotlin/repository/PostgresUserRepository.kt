package repository

import com.muquit.libsodiumjna.SodiumLibrary.cryptoPwhashStr
import com.muquit.libsodiumjna.SodiumLibrary.randomBytes
import com.muquit.libsodiumjna.SodiumUtils
import db.Users as DbUsers
import db.Token as DbToken
import exception.TokenNotFoundException
import exception.UserNotFoundException
import model.Token
import model.User
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.nio.charset.StandardCharsets
import java.util.*
import javax.sql.DataSource
import kotlin.NoSuchElementException

class PostgresUserRepository(
        private val dbUserTable: DbUsers,
        private val dbTokenTable: DbToken,
        private val tokenTTL: Int,
        private val tokenByteSize: Int,
        private val secret: String
): UserRepository {
    override fun registerUser(username: String, plaintextPassword: String) {
        val hashedPassword = cryptoPwhashStr("${secret}.$plaintextPassword".toByteArray())

        transaction {
            dbUserTable.insert {
                it[this.username] = username
                it[this.password] = hashedPassword.toByteArray(StandardCharsets.US_ASCII)
            }
        }
    }

    override fun getUserByUsername(username: String): User {
        return transaction {
            val dbUsers = dbUserTable.select {
                dbUserTable.username eq username
            }

            try {
                val user = dbUsers.first()
                User(
                        user.get(dbUserTable.username),
                        user.get(dbUserTable.password).toString(StandardCharsets.US_ASCII)
                )
            } catch(exception: NoSuchElementException) {
                throw UserNotFoundException(username)
            }
        }
    }

    override fun issueTokenForUser(username: String): Token {
        val user = getUserByUsername(username)

        val cal = Calendar.getInstance()

        val issuedAt = cal.time

        cal.add(Calendar.DATE, tokenTTL)

        val validUntil = cal.time

        val token = Token(
                username = user.username,
                token = SodiumUtils.binary2Hex(randomBytes(tokenByteSize)),
                issuedAt = issuedAt,
                validUntil = validUntil
        )

        transaction {
            dbTokenTable.insert {
                it[this.tokenValue] = token.token
                it[this.issueDate] = DateTime(token.issuedAt)
                it[this.validUntil] = DateTime(token.validUntil)
                it[this.user] = user.username
            }
        }

        return token
    }

    override fun getUserForToken(token: String): User {
        return transaction {
            val dbUsers = dbUserTable.innerJoin(dbTokenTable).select {
                dbTokenTable.tokenValue eq token and (dbTokenTable.validUntil greaterEq DateTime.now())
            }

            try {
                val user = dbUsers.first()

                User(
                        user.get(dbUserTable.username),
                        user.get(dbUserTable.password).toString()
                );
            } catch (exception: NoSuchElementException) {
                throw TokenNotFoundException(token)
            }
        }
    }
}