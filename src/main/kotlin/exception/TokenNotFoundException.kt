package exception

class TokenNotFoundException(val token: String): Exception("Token $token not found") {
}