package msa.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus
) {
    // 공통 에러
    INVALID_INPUT("COMMON_001", "잘못된 입력입니다", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("COMMON_002", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON_003", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED("COMMON_004", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    METHOD_NOT_ALLOWED("COMMON_005", "허용되지 않은 HTTP 메서드입니다", HttpStatus.METHOD_NOT_ALLOWED),

    // 인증/인가 에러
    INVALID_TOKEN("AUTH_001", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_002", "만료된 토큰입니다", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED("AUTH_003", "로그인에 실패했습니다", HttpStatus.UNAUTHORIZED),
    SIGNUP_FAILED("AUTH_004", "회원가입에 실패했습니다", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("AUTH_005", "이미 존재하는 이메일입니다", HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE("AUTH_006", "잘못된 인증 코드입니다", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("AUTH_007", "인증 코드가 만료되었습니다", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED("AUTH_008", "이메일 발송에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND("AUTH_009", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 게시물 관련 에러
    POST_NOT_FOUND("POST_001", "게시물을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    POST_CREATE_FAILED("POST_002", "게시물 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_UPDATE_FAILED("POST_003", "게시물 수정에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_DELETE_FAILED("POST_004", "게시물 삭제에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_ACCESS_DENIED("POST_005", "게시물에 대한 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 댓글 관련 에러
    COMMENT_NOT_FOUND("COMMENT_001", "댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    COMMENT_CREATE_FAILED("COMMENT_002", "댓글 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMENT_UPDATE_FAILED("COMMENT_003", "댓글 수정에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMENT_DELETE_FAILED("COMMENT_004", "댓글 삭제에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMENT_ACCESS_DENIED("COMMENT_005", "댓글에 대한 권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_COMMENT_PASSWORD("COMMENT_006", "댓글 비밀번호가 일치하지 않습니다", HttpStatus.BAD_REQUEST),

    // 데이터베이스 관련 에러
    DATABASE_CONNECTION_ERROR("DB_001", "데이터베이스 연결 오류", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_INTEGRITY_VIOLATION("DB_002", "데이터 무결성 위반", HttpStatus.CONFLICT),

    // 외부 서비스 관련 에러
    EXTERNAL_SERVICE_ERROR("EXT_001", "외부 서비스 오류", HttpStatus.BAD_GATEWAY),
    KAFKA_PUBLISH_FAILED("EXT_002", "이벤트 발행에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    REDIS_CONNECTION_ERROR("EXT_003", "Redis 연결 오류", HttpStatus.INTERNAL_SERVER_ERROR);
}