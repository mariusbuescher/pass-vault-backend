package validation.exception

class InvalidPublicKeyException(message: String, fieldName: String): ValidationException(message, fieldName) {
}