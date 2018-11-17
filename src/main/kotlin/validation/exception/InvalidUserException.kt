package validation.exception

class InvalidUserException(message: String, fieldName: String): ValidationException(message, fieldName) {
}