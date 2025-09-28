package msa.user.controller

import jakarta.validation.Valid
import msa.user.dto.*
import msa.user.model.VerificationType
import msa.user.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/send-verification-code")
    fun sendSignupVerificationCode(@Valid @RequestBody request: SendVerificationCodeRequest): ResponseEntity<ApiResponse<Unit>> {
        val response = authService.sendSignupVerificationCode(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/send-password-reset-code")
    fun sendPasswordResetVerificationCode(@Valid @RequestBody request: SendVerificationCodeRequest): ResponseEntity<ApiResponse<Unit>> {
        val response = authService.sendPasswordResetVerificationCode(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/verify-code")
    fun verifySignupCode(@Valid @RequestBody request: VerifyCodeRequest): ResponseEntity<ApiResponse<Unit>> {
        val response = authService.verifyCode(request, VerificationType.SIGNUP)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/verify-password-reset-code")
    fun verifyPasswordResetCode(@Valid @RequestBody request: VerifyCodeRequest): ResponseEntity<ApiResponse<Unit>> {
        val response = authService.verifyCode(request, VerificationType.PASSWORD_RESET)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.signup(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse<Unit>> {
        val response = authService.resetPassword(request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    @PostMapping("/find-email")
    fun findUserByEmail(@Valid @RequestBody request: SendVerificationCodeRequest): ResponseEntity<ApiResponse<String>> {
        val response = authService.findUserByEmail(request.email)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }
}