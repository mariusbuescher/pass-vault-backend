package model

import java.util.*

data class Password(
        val id: UUID,
        val name: String,
        val domain: String,
        val account: String
) {
}