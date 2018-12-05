package security

import com.muquit.libsodiumjna.SodiumLibrary
import java.nio.charset.StandardCharsets

class SodiumPasswordHasher(
        private val secret: String
): PasswordHasher {
    override fun hashPassword(plaintextPassword: String): ByteArray =
            SodiumLibrary.cryptoPwhashStr("${secret}.$plaintextPassword".toByteArray())
                    .toByteArray(StandardCharsets.US_ASCII)


    override fun verifyPassword(plaintextPassword: String, hashedPassword: String): Boolean =
            SodiumLibrary.cryptoPwhashStrVerify(
                    hashedPassword,
                    "${secret}.$plaintextPassword".toByteArray()
            )
}