package repository

import exception.PasswordNotFoundException
import exception.UserNotFoundException
import db.Passwords as DbPasswords
import db.Users as DbUsers
import model.Password
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ExposedPasswordRepository(
        private val database: Database
): PasswordRepository {
    private val mapPasswordFromDb = { row: ResultRow ->
        Password(
                id = row.get(DbPasswords.id),
                name = row.get(DbPasswords.name),
                domain = row.get(DbPasswords.domain),
                account = row.get(DbPasswords.account)
        )
    }

    override fun getPasswordsForUser(username: String): List<Password> {
        return transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            DbPasswords.select {
                DbPasswords.user eq username
            }.map(mapPasswordFromDb)
        }
    }

    override fun createPassword(username: String, name: String, domain: String, account: String): Password {
        val password = Password(
                id = UUID.randomUUID(),
                name = name,
                domain = domain,
                account = account
        )

        return transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            DbPasswords.insert {
                it[DbPasswords.id] = password.id
                it[DbPasswords.name] = password.name
                it[DbPasswords.domain] = password.domain
                it[DbPasswords.account] = password.account
                it[DbPasswords.user] = username
            }

            password
        }
    }

    override fun deletePassword(username: String, id: UUID) {
        transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            DbPasswords.deleteWhere {
                DbPasswords.id eq id and
                ( DbPasswords.user eq username )
            }
        }
    }

    override fun getPassword(username: String, id: UUID): Password {
        return transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            val passwords = DbPasswords.select {
                DbPasswords.id eq id and
                        (DbPasswords.user eq username)
            }.limit(1).map(mapPasswordFromDb)

            if (passwords.size == 0) {
                throw PasswordNotFoundException(id)
            }

            passwords.first()
        }
    }
}