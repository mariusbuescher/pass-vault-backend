package validation

import dto.UserDto
import validation.exception.InvalidUserException

class UserDtoValidator: Validator<UserDto> {
    override fun validate(dto: UserDto): Boolean {
        if (dto.username === null) {
            throw InvalidUserException("Property username must be a string", "username")
        }

        if (dto.password === null) {
            throw InvalidUserException("Property password must be a string.", "password")
        }

        return true
    }
}