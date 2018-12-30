package validation.exception

class InvalidPasswordException(message: String, fieldName: String): ValidationException(message, fieldName) {
}