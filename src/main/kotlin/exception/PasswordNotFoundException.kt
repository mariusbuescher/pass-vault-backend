package exception

import java.util.UUID

class PasswordNotFoundException(id: UUID): Exception("Password with id $id not found.") {
}