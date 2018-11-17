package exception

class UserNotFoundException(val username: String): Exception("User with username $username not found.") {
}