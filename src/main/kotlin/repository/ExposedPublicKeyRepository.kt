package repository

import db.PublicKeys as DbPublicKeys
import db.Users as DbUsers
import exception.PublicKeyNotFoundException
import exception.UserNotFoundException
import model.PublicKey
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*

class ExposedPublicKeyRepository(
        private val database: Database
): PublicKeyRepository {
    private val mapPublicKeyFromDb = { row: ResultRow ->
        PublicKey(
                id = row.get(DbPublicKeys.id),
                addedAt = row.get(DbPublicKeys.addedAt).toDate(),
                keyString = row.get(DbPublicKeys.publicKey)
        )
    }

    override fun getPublicKeys(username: String): List<PublicKey> =
            transaction(database) {
                DbPublicKeys.select {
                    DbPublicKeys.user eq username
                }.map { mapPublicKeyFromDb(it) }
            }

    override fun addPublicKey(publicKeyStr: String, username: String): PublicKey {
        val publicKey = PublicKey(
                id = UUID.randomUUID(),
                keyString = publicKeyStr,
                addedAt = DateTime.now().toDate()
        )

        transaction(database) {
            val dbUserCount = DbUsers.select {
                DbUsers.username eq username
            }.count()

            if (dbUserCount == 0) {
                throw UserNotFoundException(username)
            }

            DbPublicKeys.insert {
                it[this.id] = publicKey.id
                it[this.publicKey] = publicKey.keyString
                it[this.addedAt] = DateTime(publicKey.addedAt)
                it[this.user] = username
            }
        }

        return publicKey
    }

    override fun getPublicKey(publicKeyStr: String, username: String): PublicKey {
        return transaction(database) {
            val dbPublicKeys = DbPublicKeys.select {
                DbPublicKeys.publicKey eq publicKeyStr and
                        (DbPublicKeys.user eq username)
            }.limit(1).map(mapPublicKeyFromDb)

            try {  dbPublicKeys.first()
            } catch (exception: NoSuchElementException) {
                throw PublicKeyNotFoundException(publicKeyStr)
            }
        }
    }

    override fun getPublicKey(id: UUID, username: String): PublicKey {
        return transaction(database) {
            val dbPublicKeys = DbPublicKeys.select {
                DbPublicKeys.id eq id and
                        (DbPublicKeys.user eq username)
            }.limit(1).map(mapPublicKeyFromDb)

            try {
                dbPublicKeys.first()
            } catch (exception: NoSuchElementException) {
                throw PublicKeyNotFoundException(id)
            }
        }
    }

    override fun revokePublicKey(publicKeyStr: String, username: String) {
        transaction(database) {
            val deletedKey = DbPublicKeys.deleteWhere {
                DbPublicKeys.publicKey eq publicKeyStr and
                        (DbPublicKeys.user eq username)
            }

            if (deletedKey == 0) {
                throw PublicKeyNotFoundException(publicKeyStr)
            }
        }
    }

    override fun revokePublicKey(id: UUID, username: String) {
        transaction(database) {
            val deletedKey = DbPublicKeys.deleteWhere {
                DbPublicKeys.id eq id and
                        (DbPublicKeys.user eq username)
            }

            if (deletedKey == 0) {
                throw PublicKeyNotFoundException(id)
            }
        }
    }
}