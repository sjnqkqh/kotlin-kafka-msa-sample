package msa.user.service

import msa.user.dto.*
import msa.user.model.User
import msa.user.model.UserType
import msa.user.model.VerificationCode
import msa.user.model.VerificationType
import msa.user.repository.UserRepository
import msa.user.repository.VerificationCodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var verificationCodeRepository: VerificationCodeRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    private lateinit var authService: AuthService
    private val verificationCodeExpiration = 300000L // 5분

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userRepository = userRepository,
            verificationCodeRepository = verificationCodeRepository,
            passwordEncoder = passwordEncoder,
            jwtService = jwtService,
            emailService = emailService,
            authenticationManager = authenticationManager,
            verificationCodeExpiration = verificationCodeExpiration
        )
    }

    @Test
    @DisplayName("회원가입용 인증코드 발송 - 성공")
    fun sendSignupVerificationCode_Success() {
        // Given
        val email = "test@example.com"
        val request = SendVerificationCodeRequest(email)
        val verificationCode = "123456"

        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(emailService.generateVerificationCode()).thenReturn(verificationCode)

        // When
        val result = authService.sendSignupVerificationCode(request)

        // Then
        assertTrue(result.success)
        assertEquals("인증번호가 발송되었습니다.", result.message)

        verify(verificationCodeRepository).deleteByEmailAndType(email, VerificationType.SIGNUP)
        verify(verificationCodeRepository).save(any<VerificationCode>())
        verify(emailService).sendVerificationCode(email, verificationCode, "SIGNUP")
    }

    @Test
    @DisplayName("회원가입용 인증코드 발송 - 이미 등록된 이메일")
    fun sendSignupVerificationCode_EmailAlreadyExists() {
        // Given
        val email = "existing@example.com"
        val request = SendVerificationCodeRequest(email)

        whenever(userRepository.existsByEmail(email)).thenReturn(true)

        // When
        val result = authService.sendSignupVerificationCode(request)

        // Then
        assertFalse(result.success)
        assertEquals("이미 등록된 이메일입니다.", result.message)

        verify(verificationCodeRepository, never()).save(any<VerificationCode>())
        verify(emailService, never()).sendVerificationCode(any(), any(), any())
    }

    @Test
    @DisplayName("비밀번호 재설정용 인증코드 발송 - 성공")
    fun sendPasswordResetVerificationCode_Success() {
        // Given
        val email = "user@example.com"
        val request = SendVerificationCodeRequest(email)
        val verificationCode = "123456"

        whenever(userRepository.existsByEmail(email)).thenReturn(true)
        whenever(emailService.generateVerificationCode()).thenReturn(verificationCode)

        // When
        val result = authService.sendPasswordResetVerificationCode(request)

        // Then
        assertTrue(result.success)
        assertEquals("인증번호가 발송되었습니다.", result.message)

        verify(verificationCodeRepository).deleteByEmailAndType(email, VerificationType.PASSWORD_RESET)
        verify(verificationCodeRepository).save(any<VerificationCode>())
        verify(emailService).sendVerificationCode(email, verificationCode, "PASSWORD_RESET")
    }

    @Test
    @DisplayName("비밀번호 재설정용 인증코드 발송 - 등록되지 않은 이메일")
    fun sendPasswordResetVerificationCode_EmailNotFound() {
        // Given
        val email = "notfound@example.com"
        val request = SendVerificationCodeRequest(email)

        whenever(userRepository.existsByEmail(email)).thenReturn(false)

        // When
        val result = authService.sendPasswordResetVerificationCode(request)

        // Then
        assertFalse(result.success)
        assertEquals("등록되지 않은 이메일입니다.", result.message)

        verify(verificationCodeRepository, never()).save(any<VerificationCode>())
        verify(emailService, never()).sendVerificationCode(any(), any(), any())
    }

    @Test
    @DisplayName("인증코드 검증 - 성공")
    fun verifyCode_Success() {
        // Given
        val email = "test@example.com"
        val code = "123456"
        val request = VerifyCodeRequest(email, code)
        val verificationCode = VerificationCode(
            email = email,
            code = code,
            type = VerificationType.SIGNUP,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )

        whenever(verificationCodeRepository.findByEmailAndCodeAndType(email, code, VerificationType.SIGNUP))
            .thenReturn(Optional.of(verificationCode))

        // When
        val result = authService.verifyCode(request, VerificationType.SIGNUP)

        // Then
        assertTrue(result.success)
        assertEquals("인증이 완료되었습니다.", result.message)
    }

    @Test
    @DisplayName("인증코드 검증 - 코드 없음")
    fun verifyCode_CodeNotFound() {
        // Given
        val email = "test@example.com"
        val code = "wrong-code"
        val request = VerifyCodeRequest(email, code)

        whenever(verificationCodeRepository.findByEmailAndCodeAndType(email, code, VerificationType.SIGNUP))
            .thenReturn(Optional.empty())

        // When
        val result = authService.verifyCode(request, VerificationType.SIGNUP)

        // Then
        assertFalse(result.success)
        assertEquals("유효하지 않은 인증번호입니다.", result.message)
    }

    @Test
    @DisplayName("회원가입 - 성공")
    fun signup_Success() {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val name = "New User"
        val verificationCodeValue = "123456"
        val encodedPassword = "encoded-password"
        val token = "jwt-token"

        val request = SignupRequest(email, password, name, verificationCodeValue)
        val verificationCode = VerificationCode(
            email = email,
            code = verificationCodeValue,
            type = VerificationType.SIGNUP,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        val savedUser = User(
            id = 1L,
            email = email,
            password = encodedPassword,
            name = name,
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(verificationCodeRepository.findByEmailAndCodeAndType(email, verificationCodeValue, VerificationType.SIGNUP))
            .thenReturn(Optional.of(verificationCode))
        whenever(passwordEncoder.encode(password)).thenReturn(encodedPassword)
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(jwtService.generateToken(savedUser)).thenReturn(token)

        // When
        val result = authService.signup(request)

        // Then
        assertTrue(result.success)
        assertEquals("회원가입이 완료되었습니다.", result.message)
        assertEquals(token, result.data?.accessToken)
        assertEquals(email, result.data?.user?.email)
        assertEquals(name, result.data?.user?.name)

        verify(userRepository).save(any<User>())
        verify(verificationCodeRepository).save(verificationCode.copy(isUsed = true))
    }

    @Test
    @DisplayName("회원가입 - 이미 등록된 이메일")
    fun signup_EmailAlreadyExists() {
        // Given
        val email = "existing@example.com"
        val request = SignupRequest(email, "password123", "User", "123456")

        whenever(userRepository.existsByEmail(email)).thenReturn(true)

        // When
        val result = authService.signup(request)

        // Then
        assertFalse(result.success)
        assertEquals("이미 등록된 이메일입니다.", result.message)

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    @DisplayName("회원가입 - 유효하지 않은 인증코드")
    fun signup_InvalidVerificationCode() {
        // Given
        val email = "newuser@example.com"
        val request = SignupRequest(email, "password123", "User", "invalid-code")

        whenever(userRepository.existsByEmail(email)).thenReturn(false)
        whenever(verificationCodeRepository.findByEmailAndCodeAndType(email, "invalid-code", VerificationType.SIGNUP))
            .thenReturn(Optional.empty())

        // When
        val result = authService.signup(request)

        // Then
        assertFalse(result.success)
        assertEquals("유효하지 않은 인증번호입니다.", result.message)

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    @DisplayName("로그인 - 성공")
    fun login_Success() {
        // Given
        val email = "user@example.com"
        val password = "password123"
        val token = "jwt-token"
        val request = LoginRequest(email, password)
        val user = User(
            id = 1L,
            email = email,
            password = "encoded-password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(mock())
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(jwtService.generateToken(user)).thenReturn(token)

        // When
        val result = authService.login(request)

        // Then
        assertTrue(result.success)
        assertEquals("로그인이 완료되었습니다.", result.message)
        assertEquals(token, result.data?.accessToken)
        assertEquals(email, result.data?.user?.email)
    }

    @Test
    @DisplayName("로그인 - 인증 실패")
    fun login_AuthenticationFailed() {
        // Given
        val email = "user@example.com"
        val password = "wrongpassword"
        val request = LoginRequest(email, password)

        whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(BadCredentialsException("Bad credentials"))

        // When
        val result = authService.login(request)

        // Then
        assertFalse(result.success)
        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", result.message)

        verify(userRepository, never()).findByEmail(any())
        verify(jwtService, never()).generateToken(any())
    }

    @Test
    @DisplayName("비밀번호 재설정 - 성공")
    fun resetPassword_Success() {
        // Given
        val email = "user@example.com"
        val newPassword = "newpassword123"
        val verificationCodeValue = "123456"
        val encodedPassword = "encoded-new-password"

        val request = ResetPasswordRequest(email, newPassword, verificationCodeValue)
        val user = User(
            id = 1L,
            email = email,
            password = "old-encoded-password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val verificationCode = VerificationCode(
            email = email,
            code = verificationCodeValue,
            type = VerificationType.PASSWORD_RESET,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))
        whenever(verificationCodeRepository.findByEmailAndCodeAndType(email, verificationCodeValue, VerificationType.PASSWORD_RESET))
            .thenReturn(Optional.of(verificationCode))
        whenever(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword)

        // When
        val result = authService.resetPassword(request)

        // Then
        assertTrue(result.success)
        assertEquals("비밀번호가 재설정되었습니다.", result.message)

        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userCaptor.capture())
        assertEquals(encodedPassword, userCaptor.value.password)

        verify(verificationCodeRepository).save(verificationCode.copy(isUsed = true))
    }

    @Test
    @DisplayName("비밀번호 재설정 - 사용자 없음")
    fun resetPassword_UserNotFound() {
        // Given
        val email = "notfound@example.com"
        val request = ResetPasswordRequest(email, "newpassword123", "123456")

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())

        // When
        val result = authService.resetPassword(request)

        // Then
        assertFalse(result.success)
        assertEquals("등록되지 않은 이메일입니다.", result.message)

        verify(verificationCodeRepository, never()).findByEmailAndCodeAndType(any(), any(), any())
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    @DisplayName("이메일 찾기 - 성공")
    fun findUserByEmail_Success() {
        // Given
        val email = "testuser@example.com"
        val user = User(
            id = 1L,
            email = email,
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        // When
        val result = authService.findUserByEmail(email)

        // Then
        assertTrue(result.success)
        assertEquals("가입된 이메일을 찾았습니다.", result.message)
        assertEquals("te******@example.com", result.data)
    }

    @Test
    @DisplayName("이메일 찾기 - 사용자 없음")
    fun findUserByEmail_UserNotFound() {
        // Given
        val email = "notfound@example.com"

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())

        // When
        val result = authService.findUserByEmail(email)

        // Then
        assertFalse(result.success)
        assertEquals("등록되지 않은 이메일입니다.", result.message)
    }

    @Test
    @DisplayName("이메일 마스킹 테스트 - 짧은 이메일")
    fun testEmailMasking_ShortEmail() {
        // Given
        val email = "ab@test.com"
        val user = User(
            email = email,
            password = "password",
            name = "Test User"
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        // When
        val result = authService.findUserByEmail(email)

        // Then
        assertTrue(result.success)
        assertEquals("ab@test.com", result.data)
    }

    @Test
    @DisplayName("이메일 마스킹 테스트 - 긴 이메일")
    fun testEmailMasking_LongEmail() {
        // Given
        val email = "verylongemail@example.com"
        val user = User(
            email = email,
            password = "password",
            name = "Test User"
        )

        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        // When
        val result = authService.findUserByEmail(email)

        // Then
        assertTrue(result.success)
        assertEquals("ve***********@example.com", result.data)
    }
}