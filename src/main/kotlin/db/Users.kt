package db

import org.jetbrains.exposed.sql.Table

object Users: Table("auth_user") {
    val username = varchar("username", 255).primaryKey()
    val password = binary("password", 2048)
}