package msa.common.exception

class CustomException(
    val errorCode: ErrorCode,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null
) : RuntimeException(errorCode.message, cause) {

    val code: String get() = errorCode.code
    val httpStatus get() = errorCode.httpStatus
}