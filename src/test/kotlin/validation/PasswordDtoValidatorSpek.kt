package validation

import dto.PasswordDto
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import validation.exception.InvalidPasswordException
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

object PasswordDtoValidatorSpek: Spek({
    describe("PasswordDtoValidator") {
        it("should mark a password as valid when all fields are set") {
            val validator = PasswordDtoValidator()

            val passwordDto = PasswordDto(
                    name = "test",
                    domain = "example.com",
                    account = "test-admin"
            )

            assertTrue { validator.validate(passwordDto) }
        }

        it("should throw an exception when name is not set") {
            val validator = PasswordDtoValidator()

            val passwordDto = PasswordDto(
                    domain = "example.com",
                    account = "test-admin"
            )

            assertFailsWith<InvalidPasswordException>("Password must have a name.") {
                validator.validate(passwordDto)
            }
        }

        it("should throw an exception when domain is not set") {
            val validator = PasswordDtoValidator()

            val passwordDto = PasswordDto(
                    name = "test",
                    account = "test-admin"
            )

            assertFailsWith<InvalidPasswordException>("Password must have a domain.") {
                validator.validate(passwordDto)
            }
        }

        it("should throw an exception when account is not set") {
            val validator = PasswordDtoValidator()

            val passwordDto = PasswordDto(
                    account = "test",
                    domain = "example.com"
            )

            assertFailsWith<InvalidPasswordException>("Password must have an account.") {
                validator.validate(passwordDto)
            }
        }
    }
})