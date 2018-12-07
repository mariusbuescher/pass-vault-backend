package validation

import dto.PublicKey
import validation.exception.InvalidPublicKeyException

class PublicKeyDtoValidator: Validator<PublicKey> {
    override fun validate(dto: PublicKey): Boolean {
        if (dto.key === null) {
            throw InvalidPublicKeyException("Public key must have a filled key attribute", "key")
        }

        return true
    }
}
