package validation.exception

import java.lang.Exception

abstract class ValidationException(message: String, val fieldName: String): Exception(message) {
}