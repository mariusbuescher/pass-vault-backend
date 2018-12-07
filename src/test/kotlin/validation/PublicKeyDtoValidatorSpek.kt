package validation

import dto.PublicKey
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import validation.exception.InvalidPublicKeyException
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

object PublicKeyDtoValidatorSpek: Spek({
    describe("PublicKeyDtoValidator") {
        val publicKeyDtoValidator by memoized { PublicKeyDtoValidator() }

        it("validates to true when a public key dto with filled key is passed") {
            assertTrue { publicKeyDtoValidator.validate(PublicKey(
                    key = "1234"
            )) }
        }

        it("throws an exception when key is not filled") {
            assertFailsWith<InvalidPublicKeyException>("Public key must have a filled key attribute") {
                publicKeyDtoValidator.validate(PublicKey())
            }
        }
    }
})