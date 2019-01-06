package security

import com.muquit.libsodiumjna.SodiumLibrary
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

object SodiumPasswordHasherSpek: Spek({
    describe("Sodium password hasher") {

        SodiumLibrary.setLibraryPath(System.getenv("SODIUM_LIBRARY_PATH") ?: "/usr/lib/libsodium.so")

        it("hashes a password with a secret") {
            val passwordHasher = SodiumPasswordHasher("1234IsNotASecretDude!")

            assertTrue {
                SodiumLibrary.cryptoPwhashStrVerify(
                        passwordHasher.hashPassword("1234IsNotASecurePassword!").toString(StandardCharsets.US_ASCII),
                        "1234IsNotASecretDude!.1234IsNotASecurePassword!".toByteArray()
                )
            }
        }

        it("verifies the password with the given secret") {
            val hashedPassword = SodiumLibrary.cryptoPwhashStr("1234IsNotASecretDude!.1234IsNotASecurePassword!".toByteArray())

            val passwordHasher = SodiumPasswordHasher("1234IsNotASecretDude!")

            assertTrue {
                passwordHasher.verifyPassword("1234IsNotASecurePassword!", hashedPassword)
            }
        }
    }
})
