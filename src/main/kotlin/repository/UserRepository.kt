package repository

import model.PublicKey
import model.Token
import model.User
import java.util.*

interface UserRepository {
    fun registerUser(username: String, plaintextPassword: String)
    fun getUserByUsername(username: String): User

    fun issueTokenForUser(username: String): Token
    fun getUserForToken(token: String): User

    fun getPublicKeys(username: String): List<PublicKey>
    fun addPublicKey(publicKeyStr: String, username: String): PublicKey
    fun getPublicKey(publicKeyStr: String, username: String): PublicKey
    fun getPublicKey(id: UUID, username: String): PublicKey
    fun revokePublicKey(publicKeyStr: String, username: String)
    fun revokePublicKey(id: UUID, username: String)
}