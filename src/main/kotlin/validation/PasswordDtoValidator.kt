package validation

import dto.PasswordDto
import validation.exception.InvalidPasswordException

class PasswordDtoValidator: Validator<PasswordDto> {
    override fun validate(dto: PasswordDto): Boolean {
        if (dto.name === null) {
            throw InvalidPasswordException("Password must have a name.", "name")
        }

        if (dto.domain === null) {
            throw InvalidPasswordException("Password must have a domain.", "domain")
        }

        if (dto.account === null) {
            throw InvalidPasswordException("Password must have an account.", "account")
        }

        return true
    }
}