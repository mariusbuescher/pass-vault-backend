package security

import com.muquit.libsodiumjna.SodiumLibrary
import com.muquit.libsodiumjna.SodiumUtils

class SodiumTokenFactory(
        private val tokenByteSize: Int
): TokenFactory {
    override fun generateRandomToken(): String =
            SodiumUtils.binary2Hex(SodiumLibrary.randomBytes(tokenByteSize))
}