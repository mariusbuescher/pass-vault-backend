package repository

import model.Token
import model.User

interface UserRepository {
    fun registerUser(username: String, plaintextPassword: String)
    fun getUserByUsername(username: String): User

    fun issueTokenForUser(username: String): Token
    fun getUserForToken(token: String): User
}