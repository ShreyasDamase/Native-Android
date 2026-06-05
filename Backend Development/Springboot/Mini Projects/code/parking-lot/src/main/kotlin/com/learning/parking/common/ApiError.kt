package com.learning.parking.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)

class ConflictException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : RuntimeException(message)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ConflictException::class)
    fun conflict(ex: ConflictException, request: HttpServletRequest) =
        error(HttpStatus.CONFLICT, ex.message ?: "Conflict", request)

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, request: HttpServletRequest) =
        error(HttpStatus.NOT_FOUND, ex.message ?: "Not found", request)

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun validation(ex: Exception, request: HttpServletRequest) =
        error(HttpStatus.BAD_REQUEST, ex.message ?: "Validation failed", request)

    private fun error(status: HttpStatus, message: String, request: HttpServletRequest): ResponseEntity<ApiError> {
        return ResponseEntity.status(status).body(
            ApiError(status = status.value(), error = status.reasonPhrase, message = message, path = request.requestURI)
        )
    }
}
