package validation

interface Validator<T> {
    @Throws
    fun validate(dto: T): Boolean
}