package model

import java.util.*

data class Token(
        val username: String,
        val token: String,
        val issuedAt: Date,
        val validUntil: Date
) {
    override fun toString(): String = token
}