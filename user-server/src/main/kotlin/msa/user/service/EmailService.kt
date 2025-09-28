package msa.user.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    fun sendVerificationCode(email: String, code: String, type: String) {
        val subject = when (type) {
            "SIGNUP" -> "[MSA Sample] 회원가입 인증번호"
            "PASSWORD_RESET" -> "[MSA Sample] 비밀번호 재설정 인증번호"
            else -> "[MSA Sample] 인증번호"
        }

        val text = """
            안녕하세요!

            요청하신 인증번호는 다음과 같습니다:

            인증번호: $code

            이 인증번호는 5분간 유효합니다.

            감사합니다.
        """.trimIndent()

        val message = SimpleMailMessage().apply {
            setTo(email)
            setSubject(subject)
            setText(text)
        }

        mailSender.send(message)
    }

    fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}