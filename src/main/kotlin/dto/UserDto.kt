package dto

data class UserDto(
        val username: String?,

        /**
         * Password in plain text!!!
         */
        val password: String?
)