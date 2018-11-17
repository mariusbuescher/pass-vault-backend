package validation

import dto.UserDto
import kotlin.test.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import validation.exception.InvalidUserException
import kotlin.test.assertFailsWith

object UserDtoValidatorSpek: Spek({
    describe("UserDtoValidator") {
        val userDtoValidator by memoized { UserDtoValidator() }
        it("validates to true when a fully described UserDto is passed") {
            assertTrue {
                userDtoValidator.validate(UserDto("test", "1234"))
            }
        }

        it("throws an InvalidUserException when no username is set") {
            assertFailsWith(InvalidUserException::class, "Property username must be a string") {
                userDtoValidator.validate(UserDto(username = null, password = "1234"))
            }
        }

        it("throws an InvalidUserException when no password is set") {
            assertFailsWith(InvalidUserException::class, "Property password must be a string") {
                userDtoValidator.validate(UserDto(username = "test", password = null))
            }
        }
    }
})