package msa.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import msa.user.dto.*
import msa.user.model.User
import msa.user.model.UserType
import msa.user.model.VerificationCode
import msa.user.model.VerificationType
import msa.user.repository.UserRepository
import msa.user.repository.VerificationCodeRepository
import msa.user.service.EmailService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var verificationCodeRepository: VerificationCodeRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        verificationCodeRepository.deleteAll()

        // EmailService mock 설정
        whenever(emailService.generateVerificationCode()).thenReturn("123456")
    }

    @Test
    @DisplayName("회원가입용 인증코드 발송 - 성공")
    fun sendSignupVerificationCode_Success() {
        val request = SendVerificationCodeRequest(email = "test@example.com")

        mockMvc.perform(
            post("/api/auth/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("회원가입용 인증코드 발송 - 이미 등록된 이메일")
    fun sendSignupVerificationCode_EmailAlreadyExists() {
        // 기존 사용자 생성
        val existingUser = User(
            email = "existing@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Existing User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(existingUser)

        val request = SendVerificationCodeRequest(email = "existing@example.com")

        mockMvc.perform(
            post("/api/auth/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("이미 존재하는 이메일입니다"))
    }

    @Test
    @DisplayName("회원가입용 인증코드 발송 - 유효하지 않은 이메일 형식")
    fun sendSignupVerificationCode_InvalidEmailFormat() {
        val request = SendVerificationCodeRequest(email = "invalid-email")

        mockMvc.perform(
            post("/api/auth/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("비밀번호 재설정용 인증코드 발송 - 성공")
    fun sendPasswordResetVerificationCode_Success() {
        // 기존 사용자 생성
        val existingUser = User(
            email = "user@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(existingUser)

        val request = SendVerificationCodeRequest(email = "user@example.com")

        mockMvc.perform(
            post("/api/auth/send-password-reset-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("비밀번호 재설정용 인증코드 발송 - 등록되지 않은 이메일")
    fun sendPasswordResetVerificationCode_EmailNotFound() {
        val request = SendVerificationCodeRequest(email = "notfound@example.com")

        mockMvc.perform(
            post("/api/auth/send-password-reset-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다"))
    }

    @Test
    @DisplayName("인증코드 검증 - 성공")
    fun verifyCode_Success() {
        // 인증코드 생성
        val verificationCode = VerificationCode(
            email = "test@example.com",
            code = "123456",
            type = VerificationType.SIGNUP,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        verificationCodeRepository.save(verificationCode)

        val request = VerifyCodeRequest(
            email = "test@example.com",
            code = "123456"
        )

        mockMvc.perform(
            post("/api/auth/verify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("인증코드 검증 - 잘못된 코드")
    fun verifyCode_InvalidCode() {
        val request = VerifyCodeRequest(
            email = "test@example.com",
            code = "wrong-code"
        )

        mockMvc.perform(
            post("/api/auth/verify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("잘못된 인증 코드입니다"))
    }

    @Test
    @DisplayName("회원가입 - 성공")
    fun signup_Success() {
        // 유효한 인증코드 생성
        val verificationCode = VerificationCode(
            email = "newuser@example.com",
            code = "123456",
            type = VerificationType.SIGNUP,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        verificationCodeRepository.save(verificationCode)

        val request = SignupRequest(
            email = "newuser@example.com",
            password = "password123",
            name = "New User",
            verificationCode = "123456"
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.data.user.name").value("New User"))
    }

    @Test
    @DisplayName("회원가입 - 이미 등록된 이메일")
    fun signup_EmailAlreadyExists() {
        // 기존 사용자 생성
        val existingUser = User(
            email = "existing@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Existing User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(existingUser)

        val request = SignupRequest(
            email = "existing@example.com",
            password = "newpassword123",
            name = "New User",
            verificationCode = "123456"
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("이미 존재하는 이메일입니다"))
    }

    @Test
    @DisplayName("회원가입 - 유효하지 않은 인증코드")
    fun signup_InvalidVerificationCode() {
        val request = SignupRequest(
            email = "newuser@example.com",
            password = "password123",
            name = "New User",
            verificationCode = "invalid-code"
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("잘못된 인증 코드입니다"))
    }

    @Test
    @DisplayName("회원가입 - 비밀번호 길이 부족")
    fun signup_PasswordTooShort() {
        val request = SignupRequest(
            email = "newuser@example.com",
            password = "123",
            name = "New User",
            verificationCode = "123456"
        )

        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("로그인 - 성공")
    fun login_Success() {
        // 사용자 생성
        val user = User(
            email = "user@example.com",
            password = passwordEncoder.encode("password123"),
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(user)

        val request = LoginRequest(
            email = "user@example.com",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호")
    fun login_WrongPassword() {
        // 사용자 생성
        val user = User(
            email = "user@example.com",
            password = passwordEncoder.encode("correctpassword"),
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(user)

        val request = LoginRequest(
            email = "user@example.com",
            password = "wrongpassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인에 실패했습니다"))
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 사용자")
    fun login_UserNotFound() {
        val request = LoginRequest(
            email = "notfound@example.com",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("로그인에 실패했습니다"))
    }

    @Test
    @DisplayName("로그인 - 잘못된 이메일 형식")
    fun login_InvalidEmailFormat() {
        val request = LoginRequest(
            email = "invalid-email",
            password = "password123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("비밀번호 재설정 - 성공")
    fun resetPassword_Success() {
        // 사용자 생성
        val user = User(
            email = "user@example.com",
            password = passwordEncoder.encode("oldpassword"),
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        userRepository.save(user)

        // 인증코드 생성
        val verificationCode = VerificationCode(
            email = "user@example.com",
            code = "123456",
            type = VerificationType.PASSWORD_RESET,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        verificationCodeRepository.save(verificationCode)

        val request = ResetPasswordRequest(
            email = "user@example.com",
            newPassword = "newpassword123",
            verificationCode = "123456"
        )

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("비밀번호 재설정 - 등록되지 않은 이메일")
    fun resetPassword_EmailNotFound() {
        val request = ResetPasswordRequest(
            email = "notfound@example.com",
            newPassword = "newpassword123",
            verificationCode = "123456"
        )

        mockMvc.perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다"))
    }
}