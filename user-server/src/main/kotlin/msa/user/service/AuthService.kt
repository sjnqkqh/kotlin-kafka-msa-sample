package msa.user.service

import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
import msa.user.dto.AuthResponse
import msa.user.dto.UserInfo
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
    private val tokenRedisService: TokenRedisService,
    @Value("\${verification.code.expiration}")
    private val verificationCodeExpiration: Long
) {

    @Transactional
    fun sendSignupVerificationCode(email: String) {
        if (userRepository.existsByEmail(email)) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        sendVerificationCodeInternal(email, VerificationType.SIGNUP)
    }

    @Transactional
    fun sendPasswordResetVerificationCode(email: String) {
        if (!userRepository.existsByEmail(email)) {
            throw CustomException(ErrorCode.USER_NOT_FOUND)
        }

        sendVerificationCodeInternal(email, VerificationType.PASSWORD_RESET)
    }

    private fun sendVerificationCodeInternal(email: String, type: VerificationType) {
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
    }

    fun verifyCode(email: String, code: String, type: VerificationType) {
        val verificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            email, code, type
        ).orElse(null)

        if (verificationCode == null || !verificationCode.isValid()) {
            throw CustomException(ErrorCode.INVALID_VERIFICATION_CODE)
        }
    }

    @Transactional
    fun signup(
        email: String,
        password: String,
        name: String,
        verificationCode: String
    ): AuthResponse {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        // 인증번호 검증
        val storedVerificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            email, verificationCode, VerificationType.SIGNUP
        ).orElse(null)

        if (storedVerificationCode == null || !storedVerificationCode.isValid()) {
            throw CustomException(ErrorCode.INVALID_VERIFICATION_CODE)
        }

        // 사용자 생성
        val user = User(
            email = email,
            password = passwordEncoder.encode(password),
            name = name,
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        val savedUser = userRepository.save(user)

        // 인증번호 사용 처리
        verificationCodeRepository.save(storedVerificationCode.copy(isUsed = true))

        // JWT 토큰 생성
        val token = jwtService.generateToken(savedUser)

        // Redis에 토큰 저장
        tokenRedisService.storeToken(savedUser.id, token)

        val userInfo = UserInfo(
            id = savedUser.id,
            email = savedUser.email,
            name = savedUser.name,
            userType = savedUser.userType
        )

        return AuthResponse(
            accessToken = token,
            user = userInfo
        )
    }

    fun login(email: String, password: String): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(email, password)
            )
        } catch (_: Exception) {
            throw CustomException(ErrorCode.LOGIN_FAILED)
        }

        val user = userRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        val token = jwtService.generateToken(user)

        // Redis에 토큰 저장
        tokenRedisService.storeToken(user.id, token)

        val userInfo = UserInfo(
            id = user.id,
            email = user.email,
            name = user.name,
            userType = user.userType
        )

        return AuthResponse(
            accessToken = token,
            user = userInfo
        )
    }

    @Transactional
    fun resetPassword(
        email: String,
        newPassword: String,
        verificationCode: String
    ) {
        // 사용자 존재 확인
        val user = userRepository.findByEmail(email).orElse(null)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        // 인증번호 검증
        val storedVerificationCode = verificationCodeRepository.findByEmailAndCodeAndType(
            email, verificationCode, VerificationType.PASSWORD_RESET
        ).orElse(null)

        if (storedVerificationCode == null || !storedVerificationCode.isValid()) {
            throw CustomException(ErrorCode.INVALID_VERIFICATION_CODE)
        }

        // 비밀번호 업데이트
        val updatedUser = user.copy(
            password = passwordEncoder.encode(newPassword),
            updatedAt = LocalDateTime.now()
        )
        userRepository.save(updatedUser)

        // 인증번호 사용 처리
        verificationCodeRepository.save(storedVerificationCode.copy(isUsed = true))
    }

}