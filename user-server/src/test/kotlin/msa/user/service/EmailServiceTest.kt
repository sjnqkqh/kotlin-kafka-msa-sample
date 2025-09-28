package msa.user.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.verify
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {

    @Mock
    private lateinit var mailSender: JavaMailSender

    private lateinit var emailService: EmailService

    @BeforeEach
    fun setUp() {
        emailService = EmailService(mailSender)
        clearInvocations(mailSender)
    }

    @Test
    @DisplayName("회원가입 인증코드 이메일 발송 - 성공")
    fun sendVerificationCode_Signup_Success() {
        // Given
        val email = "test@example.com"
        val code = "123456"
        val type = "SIGNUP"

        // When
        emailService.sendVerificationCode(email, code, type)

        // Then
        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(messageCaptor.capture())

        val sentMessage = messageCaptor.value
        assertEquals(email, sentMessage.to?.get(0))
        assertEquals("[MSA Sample] 회원가입 인증번호", sentMessage.subject)
        assertTrue(sentMessage.text?.contains("인증번호: $code") == true)
        assertTrue(sentMessage.text?.contains("5분간 유효") == true)
    }

    @Test
    @DisplayName("비밀번호 재설정 인증코드 이메일 발송 - 성공")
    fun sendVerificationCode_PasswordReset_Success() {
        // Given
        val email = "user@example.com"
        val code = "654321"
        val type = "PASSWORD_RESET"

        // When
        emailService.sendVerificationCode(email, code, type)

        // Then
        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(messageCaptor.capture())

        val sentMessage = messageCaptor.value
        assertEquals(email, sentMessage.to?.get(0))
        assertEquals("[MSA Sample] 비밀번호 재설정 인증번호", sentMessage.subject)
        assertTrue(sentMessage.text?.contains("인증번호: $code") == true)
        assertTrue(sentMessage.text?.contains("5분간 유효") == true)
    }

    @Test
    @DisplayName("기본 인증코드 이메일 발송 - 알 수 없는 타입")
    fun sendVerificationCode_DefaultType_Success() {
        // Given
        val email = "user@example.com"
        val code = "999999"
        val type = "UNKNOWN_TYPE"

        // When
        emailService.sendVerificationCode(email, code, type)

        // Then
        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(messageCaptor.capture())

        val sentMessage = messageCaptor.value
        assertEquals(email, sentMessage.to?.get(0))
        assertEquals("[MSA Sample] 인증번호", sentMessage.subject)
        assertTrue(sentMessage.text?.contains("인증번호: $code") == true)
    }

    @Test
    @DisplayName("인증코드 생성 - 6자리 숫자")
    fun generateVerificationCode_SixDigits() {
        // When
        val code = emailService.generateVerificationCode()

        // Then
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
        assertTrue(code.toInt() >= 100000)
        assertTrue(code.toInt() <= 999999)
    }

    @Test
    @DisplayName("인증코드 생성 - 고유성 테스트")
    fun generateVerificationCode_Uniqueness() {
        // When
        val codes = mutableSetOf<String>()
        repeat(100) {
            codes.add(emailService.generateVerificationCode())
        }

        // Then
        // 100번 생성해서 모두 다른 코드가 생성되어야 함 (확률적으로 거의 불가능하지만 중복 가능성 있음)
        // 최소한 50개 이상은 서로 다른 코드여야 함
        assertTrue(codes.size >= 50, "Generated codes should be reasonably unique")
    }

    @Test
    @DisplayName("이메일 내용 검증 - 회원가입")
    fun verifyEmailContent_Signup() {
        // Given
        val email = "test@example.com"
        val code = "123456"
        val type = "SIGNUP"

        // When
        emailService.sendVerificationCode(email, code, type)

        // Then
        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(messageCaptor.capture())

        val sentMessage = messageCaptor.value
        val content = sentMessage.text ?: ""

        assertTrue(content.contains("안녕하세요!"))
        assertTrue(content.contains("요청하신 인증번호는 다음과 같습니다:"))
        assertTrue(content.contains("인증번호: $code"))
        assertTrue(content.contains("이 인증번호는 5분간 유효합니다."))
        assertTrue(content.contains("감사합니다."))
    }

    @Test
    @DisplayName("이메일 내용 검증 - 비밀번호 재설정")
    fun verifyEmailContent_PasswordReset() {
        // Given
        val email = "user@example.com"
        val code = "654321"
        val type = "PASSWORD_RESET"

        // When
        emailService.sendVerificationCode(email, code, type)

        // Then
        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(mailSender).send(messageCaptor.capture())

        val sentMessage = messageCaptor.value
        val content = sentMessage.text ?: ""

        assertTrue(content.contains("안녕하세요!"))
        assertTrue(content.contains("요청하신 인증번호는 다음과 같습니다:"))
        assertTrue(content.contains("인증번호: $code"))
        assertTrue(content.contains("이 인증번호는 5분간 유효합니다."))
        assertTrue(content.contains("감사합니다."))
    }

    @Test
    @DisplayName("여러 타입의 이메일 제목 검증")
    fun verifyEmailSubjects() {
        val testCases = listOf(
            "SIGNUP" to "[MSA Sample] 회원가입 인증번호",
            "PASSWORD_RESET" to "[MSA Sample] 비밀번호 재설정 인증번호",
            "OTHER" to "[MSA Sample] 인증번호"
        )

        testCases.forEachIndexed { index, (type, expectedSubject) ->
            // Given
            clearInvocations(mailSender)
            val email = "test${index}@example.com"
            val code = "12345${index}"

            // When
            emailService.sendVerificationCode(email, code, type)

            // Then
            val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
            verify(mailSender).send(messageCaptor.capture())

            val sentMessage = messageCaptor.value
            assertEquals(expectedSubject, sentMessage.subject)
        }
    }
}