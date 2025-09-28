package msa.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import msa.user.model.UserType

data class SignupRequest(
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,

    @field:NotBlank(message = "인증 코드는 필수입니다")
    val verificationCode: String
)

data class LoginRequest(
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

data class SendVerificationCodeRequest(
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String
)

data class VerifyCodeRequest(
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "인증 코드는 필수입니다")
    val code: String
)

data class ResetPasswordRequest(
    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    val newPassword: String,

    @field:NotBlank(message = "인증 코드는 필수입니다")
    val verificationCode: String
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val user: UserInfo
)

data class UserInfo(
    val id: Long,
    val email: String,
    val name: String,
    val userType: UserType
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)