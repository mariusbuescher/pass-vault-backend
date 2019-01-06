package security

interface TokenFactory {
    fun generateRandomToken(): String
}