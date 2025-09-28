package msa.user.service

import msa.user.dto.*
import msa.user.model.User
import msa.user.model.UserType
import msa.user.model.VerificationCode
import msa.user.model.VerificationType
import msa.user.repository.UserRepository
import msa.user.repository.VerificationCodeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val verificationCodeRepository: VerificationCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val authenticationManager: AuthenticationManager,
    @Value("\${verification.code.expiration}")
    private val verificationCodeExpiration: Long
) {

    @Transactional
    fun sendSignupVerificationCode(request: SendVerificationCodeRequest): ApiResponse<Unit> {
        if (userRepository.existsByEmail(request.email)) {
            return ApiResponse(false, "이미 등록된 이메일입니다.")
        }

        return sendVerificationCodeInternal(request.email, VerificationType.SIGNUP)
    }

    @Transactional
    fun sendPasswordResetVerificationCode(request: SendVerificationCodeRequest): ApiResponse<Unit> {
        if (!userRepository.existsByEmail(request.email)) {
            return ApiResponse(false, "등록되지 않은 이메일입니다.")
        }

        return sendVerificationCodeInternal(request.email, VerificationType.PASSWORD_RESET)
    }

    private fun sendVerificationCodeInternal(email: String, type: VerificationType): ApiResponse<Unit> {
        // 기존 인증코드 삭제
        verificationCodeRepository.deleteByEmailAndType(email, type)

        // 새 인증코드 생성
        val code = emailService.generateVerificationCode()
        val expiresAt = LocalDateTime.now().plusSeconds(verificationCodeExpiration / 1000)

        val verificationCode = VerificationCode(
            email = email,
            code = code,
            type = type,
            expiresAt = expiresAt
        )

        verificationCodeRepository.save(verificationCode)

        // 이메일 발송
        emailService.sendVerificationCode(email, code, type.name)

        return ApiResponse(true, "인증번호가 발송되었습니다.")
    }

    fun verifyCode(request: VerifyCodeRequest, type: VerificationType): ApiResponse<Unit> {
        val verificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            request.email, request.code, type
        ).orElse(null)

        if (verificationCode == null || !verificationCode.isValid()) {
            return ApiResponse(false, "유효하지 않은 인증번호입니다.")
        }

        return ApiResponse(true, "인증이 완료되었습니다.")
    }

    @Transactional
    fun signup(request: SignupRequest): ApiResponse<AuthResponse> {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email)) {
            return ApiResponse(false, "이미 등록된 이메일입니다.")
        }

        // 인증번호 검증
        val verificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            request.email, request.verificationCode, VerificationType.SIGNUP
        ).orElse(null)

        if (verificationCode == null || !verificationCode.isValid()) {
            return ApiResponse(false, "유효하지 않은 인증번호입니다.")
        }

        // 사용자 생성
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        val savedUser = userRepository.save(user)

        // 인증번호 사용 처리
        verificationCodeRepository.save(verificationCode.copy(isUsed = true))

        // JWT 토큰 생성
        val token = jwtService.generateToken(savedUser)
        val userInfo = UserInfo(
            id = savedUser.id,
            email = savedUser.email,
            name = savedUser.name,
            userType = savedUser.userType
        )

        val authResponse = AuthResponse(
            accessToken = token,
            user = userInfo
        )

        return ApiResponse(true, "회원가입이 완료되었습니다.", authResponse)
    }

    fun login(request: LoginRequest): ApiResponse<AuthResponse> {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )

            val user = userRepository.findByEmail(request.email)
                .orElseThrow { RuntimeException("사용자를 찾을 수 없습니다.") }

            val token = jwtService.generateToken(user)
            val userInfo = UserInfo(
                id = user.id,
                email = user.email,
                name = user.name,
                userType = user.userType
            )

            val authResponse = AuthResponse(
                accessToken = token,
                user = userInfo
            )

            return ApiResponse(true, "로그인이 완료되었습니다.", authResponse)
        } catch (e: Exception) {
            return ApiResponse(false, "이메일 또는 비밀번호가 올바르지 않습니다.")
        }
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest): ApiResponse<Unit> {
        // 사용자 존재 확인
        val user = userRepository.findByEmail(request.email).orElse(null)
            ?: return ApiResponse(false, "등록되지 않은 이메일입니다.")

        // 인증번호 검증
        val verificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            request.email, request.verificationCode, VerificationType.PASSWORD_RESET
        ).orElse(null)

        if (verificationCode == null || !verificationCode.isValid()) {
            return ApiResponse(false, "유효하지 않은 인증번호입니다.")
        }

        // 비밀번호 업데이트
        val updatedUser = user.copy(
            password = passwordEncoder.encode(request.newPassword),
            updatedAt = LocalDateTime.now()
        )
        userRepository.save(updatedUser)

        // 인증번호 사용 처리
        verificationCodeRepository.save(verificationCode.copy(isUsed = true))

        return ApiResponse(true, "비밀번호가 재설정되었습니다.")
    }

    fun findUserByEmail(email: String): ApiResponse<String> {
        val user = userRepository.findByEmail(email).orElse(null)
            ?: return ApiResponse(false, "등록되지 않은 이메일입니다.")

        // 이메일의 일부를 마스킹하여 반환
        val maskedEmail = maskEmail(email)
        return ApiResponse(true, "가입된 이메일을 찾았습니다.", maskedEmail)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val localPart = parts[0]
        val domain = parts[1]

        val maskedLocal = if (localPart.length <= 2) {
            localPart
        } else {
            localPart.take(2) + "*".repeat(localPart.length - 2)
        }

        return "$maskedLocal@$domain"
    }
}