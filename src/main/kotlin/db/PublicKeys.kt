package db

import org.jetbrains.exposed.sql.Table

object PublicKeys: Table("crypt_pub_key") {
    val id = uuid("id").primaryKey()
    val publicKey = text("public_key")
    val addedAt = datetime("added_at")
    val user = varchar("user_id", 255).references(Users.username)
}