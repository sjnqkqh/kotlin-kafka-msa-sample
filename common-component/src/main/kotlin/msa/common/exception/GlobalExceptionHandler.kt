package msa.common.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import msa.common.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(
        ex: CustomException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "Custom exception occurred - Code: {}, Message: {}, TraceId: {}, Path: {}",
            ex.code, ex.message, traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ex.code,
            message = ex.message ?: "알 수 없는 오류가 발생했습니다",
            details = ex.details,
            traceId = traceId
        )

        return ResponseEntity.status(ex.httpStatus).body(response)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "Validation exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = if (error is FieldError) error.field else error.objectName
            fieldName to (error.defaultMessage ?: "잘못된 값입니다")
        }

        val response = ApiResponse.validationError<Unit>(
            message = "입력 값이 올바르지 않습니다",
            details = mapOf("validationErrors" to errors),
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "Constraint violation exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val errors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }

        val response = ApiResponse.validationError<Unit>(
            message = "입력 값이 올바르지 않습니다",
            details = mapOf("validationErrors" to errors),
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "HTTP message not readable exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.INVALID_INPUT.code,
            message = "요청 데이터 형식이 올바르지 않습니다",
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "Method argument type mismatch exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.INVALID_INPUT.code,
            message = "요청 파라미터 타입이 올바르지 않습니다",
            details = mapOf("parameter" to (ex.name), "expectedType" to (ex.requiredType?.simpleName ?: "unknown")),
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "HTTP request method not supported exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.METHOD_NOT_ALLOWED.code,
            message = ErrorCode.METHOD_NOT_ALLOWED.message,
            details = mapOf(
                "method" to (ex.method),
                "supportedMethods" to (ex.supportedMethods?.toList() ?: emptyList<String>())
            ),
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response)
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "No handler found exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.RESOURCE_NOT_FOUND.code,
            message = "요청한 리소스를 찾을 수 없습니다",
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.warn(
            "Illegal argument exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.INVALID_INPUT.code,
            message = ex.message ?: ErrorCode.INVALID_INPUT.message,
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val traceId = generateTraceId()

        logger.error(
            "Unexpected exception occurred - TraceId: {}, Path: {}",
            traceId, request.requestURI, ex
        )

        val response = ApiResponse.error<Unit>(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            traceId = traceId
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    private fun generateTraceId(): String {
        return UUID.randomUUID().toString()
    }
}