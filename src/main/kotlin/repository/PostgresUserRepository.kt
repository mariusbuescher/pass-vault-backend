package repository

import com.muquit.libsodiumjna.SodiumLibrary.cryptoPwhashStr
import com.muquit.libsodiumjna.SodiumLibrary.randomBytes
import com.muquit.libsodiumjna.SodiumUtils
import exception.TokenNotFoundException
import exception.UserNotFoundException
import model.Token
import model.User
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.Timestamp
import java.util.*

class PostgresUserRepository(
        private val dbConnection: Connection,
        private val tokenTTL: Int,
        private val tokenByteSize: Int,
        private val secret: String
): UserRepository {
    override fun registerUser(username: String, plaintextPassword: String) {
        val insertStmt = dbConnection.prepareStatement(
                "INSERT INTO auth_user (username, password) values (?, ?)"
        )

        insertStmt.setString(1, username)
        val hashedPassword = cryptoPwhashStr("${secret}.$plaintextPassword".toByteArray())
        insertStmt.setBytes(2, hashedPassword.toByteArray(StandardCharsets.US_ASCII))

        insertStmt.execute()
    }

    override fun getUserByUsername(username: String): User {
        val dbUserStmt = dbConnection.prepareStatement("SELECT username, password FROM auth_user WHERE username=?")
        dbUserStmt.setString(1, username)

        val dbUsers = dbUserStmt.executeQuery()

        if (dbUsers.next()) {
            return User(dbUsers.getString("username"), dbUsers.getBytes("password").toString(StandardCharsets.US_ASCII))
        } else {
            throw UserNotFoundException(username)
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

        val tokenInsertStmt = dbConnection.prepareStatement(
                "INSERT INTO auth_token (token, issue_date, valid_until, user_id) VALUES (?, ?, ?, ?)"
        )

        tokenInsertStmt.setString(1, token.token)
        tokenInsertStmt.setTimestamp(2, Timestamp(token.issuedAt.time))
        tokenInsertStmt.setTimestamp(3, Timestamp(token.validUntil.time))
        tokenInsertStmt.setString(4, token.username)

        tokenInsertStmt.execute()

        return token
    }

    override fun getUserForToken(token: String): User {
        val dbUserSelectStmt = dbConnection.prepareStatement(
                """SELECT auth_user.username as username, auth_user.password as password
                    |FROM auth_user
                    |INNER JOIN auth_token
                    |ON auth_token.user_id=auth_user.username
                    |WHERE auth_token.token=?
                    |AND auth_token.valid_until >= ?
                """.trimMargin()
        )
        dbUserSelectStmt.setString(1, token)
        dbUserSelectStmt.setTimestamp(2, Timestamp(System.currentTimeMillis()))

        val dbUsers = dbUserSelectStmt.executeQuery()

        if (dbUsers.next()) {
            return User(dbUsers.getString("username"), dbUsers.getString("password"))
        } else {
            throw TokenNotFoundException(token)
        }
    }
}