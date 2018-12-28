package repository

import model.PublicKey
import java.util.*

interface PublicKeyRepository {
    fun getPublicKeys(username: String): List<PublicKey>
    fun addPublicKey(publicKeyStr: String, username: String): PublicKey
    fun getPublicKey(publicKeyStr: String, username: String): PublicKey
    fun getPublicKey(id: UUID, username: String): PublicKey
    fun revokePublicKey(publicKeyStr: String, username: String)
    fun revokePublicKey(id: UUID, username: String)
}