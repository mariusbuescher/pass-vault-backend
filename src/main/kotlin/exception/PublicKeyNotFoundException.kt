package exception

import java.util.*

class PublicKeyNotFoundException: Exception {
    constructor(id: UUID): super("Public key with id ${id} not found")

    constructor(publicKeyStr: String): super("Public key ${publicKeyStr} not found")
}