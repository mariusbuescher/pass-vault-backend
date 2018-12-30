package db

import org.jetbrains.exposed.sql.Table

object Passwords: Table("password") {
    val id = uuid("id").primaryKey()
    val name = varchar("name", 255)
    val domain = varchar("domain", 1023)
    val account = varchar("account", 255)
    val user = varchar("user_id", 255).references(Users.username)
}