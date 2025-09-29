package msa.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val traceId: String? = null
) {
    companion object {
        fun <T> success(data: T? = null, traceId: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                traceId = traceId
            )
        }

        fun <T> error(
            code: String,
            message: String,
            details: Map<String, Any>? = null,
            traceId: String? = null
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetail(
                    code = code,
                    message = message,
                    details = details
                ),
                traceId = traceId
            )
        }

        fun <T> validationError(
            message: String,
            details: Map<String, Any>? = null,
            traceId: String? = null
        ): ApiResponse<T> {
            return error(
                code = "VALIDATION_ERROR",
                message = message,
                details = details,
                traceId = traceId
            )
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)