package dto

data class ValidationErrorExceptionDto(
        val message: String,
        val property: String
)