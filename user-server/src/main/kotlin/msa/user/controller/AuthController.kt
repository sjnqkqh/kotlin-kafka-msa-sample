package msa.user.controller

import jakarta.validation.Valid
import msa.common.dto.ApiResponse
import msa.user.dto.*
import msa.user.model.VerificationType
import msa.user.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/send-verification-code")
    fun sendSignupVerificationCode(@Valid @RequestBody request: SendVerificationCodeRequest): ApiResponse<Unit> {
        authService.sendSignupVerificationCode(request.email)
        return ApiResponse.success()
    }

    @PostMapping("/send-password-reset-code")
    fun sendPasswordResetVerificationCode(@Valid @RequestBody request: SendVerificationCodeRequest): ApiResponse<Unit> {
        authService.sendPasswordResetVerificationCode(request.email)
        return ApiResponse.success()
    }

    @PostMapping("/verify-code")
    fun verifySignupCode(@Valid @RequestBody request: VerifyCodeRequest): ApiResponse<Unit> {
        authService.verifyCode(request.email, request.code, VerificationType.SIGNUP)
        return ApiResponse.success()
    }

    @PostMapping("/verify-password-reset-code")
    fun verifyPasswordResetCode(@Valid @RequestBody request: VerifyCodeRequest): ApiResponse<Unit> {
        authService.verifyCode(request.email, request.code, VerificationType.PASSWORD_RESET)
        return ApiResponse.success()
    }

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ApiResponse<AuthResponse> {
        val authResponse = authService.signup(
            email = request.email,
            password = request.password,
            name = request.name,
            verificationCode = request.verificationCode
        )
        return ApiResponse.success(authResponse)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ApiResponse<AuthResponse> {
        val authResponse = authService.login(
            email = request.email,
            password = request.password
        )
        return ApiResponse.success(authResponse)
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ApiResponse<Unit> {
        authService.resetPassword(
            email = request.email,
            newPassword = request.newPassword,
            verificationCode = request.verificationCode
        )
        return ApiResponse.success()
    }
}