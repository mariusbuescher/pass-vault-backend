package dto

import java.util.*

data class TokenDto(
        val token: String,
        val validUntil: Date,
        val issuedAt: Date
)