package dto

import java.util.*

data class PublicKey(
        val id: UUID? = null,
        val key: String? = null,
        val addedAt: Date? = null
)
