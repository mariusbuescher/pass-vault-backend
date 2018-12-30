package repository

import model.Password
import java.util.*

interface PasswordRepository {
    fun getPasswordsForUser(username: String): List<Password>
    fun createPassword(username: String, name: String, domain: String, account: String): Password
    fun deletePassword(username: String, id: UUID)
    fun getPassword(username: String, id: UUID): Password
}