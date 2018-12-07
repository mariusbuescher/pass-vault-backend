package model

import java.util.*

data class PublicKey(
        val id: UUID,
        val keyString: String,
        val addedAt: Date
) {
    override fun toString(): String = keyString
}
