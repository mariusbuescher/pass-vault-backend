package authentication

import io.ktor.auth.Principal

data class UserPrincipal(
        val username: String
): Principal
