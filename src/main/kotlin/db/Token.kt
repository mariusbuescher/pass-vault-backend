package db

import org.jetbrains.exposed.sql.Table

object Token: Table("auth_token") {
    val tokenValue = text("token").primaryKey()
    val issueDate = datetime("issue_date")
    val validUntil = datetime("valid_until")
    val user = varchar("user_id", 255).references(Users.username)
}