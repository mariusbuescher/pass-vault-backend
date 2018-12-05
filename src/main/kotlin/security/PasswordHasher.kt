package security

interface PasswordHasher {
    fun hashPassword(plaintextPassword: String): ByteArray

    fun verifyPassword(plaintextPassword: String, hashedPassword: String): Boolean
}