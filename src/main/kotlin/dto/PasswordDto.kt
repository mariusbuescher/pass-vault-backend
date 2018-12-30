package dto

import java.util.*

data class PasswordDto(
        val id: UUID? = null,
        val name: String? = null,
        val domain: String? = null,
        val account: String? = null
) {
}